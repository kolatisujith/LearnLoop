'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');
const puppeteer = require('C:/Users/prath/AppData/Local/npm-cache/_npx/4b4c857f6efdfb61/node_modules/puppeteer');

const base = process.env.PHASE14_BASE_URL || 'http://localhost:8080/learnloop';
const teacherEmail = process.env.PHASE14_TEACHER_EMAIL || 'teacher@learnloop.local';
const teacherPassword = process.env.PHASE14_TEACHER_PASSWORD || (() => {
  const documentation = fs.readFileSync(path.resolve(__dirname, '..', 'README.md'), 'utf8');
  const match = documentation.match(/All seeded teacher and student accounts use the development-only password:\s*```text\s*([^\r\n]+)\s*```/);
  return match ? match[1].trim() : '';
})();
const studentEmail = process.env.PHASE14_STUDENT_EMAIL;
const studentPassword = process.env.PHASE14_STUDENT_PASSWORD;
const adminEmail = process.env.PHASE14_ADMIN_EMAIL;
const adminPassword = process.env.PHASE14_ADMIN_PASSWORD;
const materialTitle = process.env.PHASE14_MATERIAL_TITLE ||
  ('PHASE14 Browser File ' + new Date().toISOString().replace(/\D/g, '') + ' ' + Math.random().toString(16).slice(2));
const reportPath = path.resolve(__dirname, '..', 'docs', 'phase14-browser-validation.md');
const checks = [];
const runtimeErrors = [];
const cspViolations = [];
const jitsiApiResponses = [];
let browser;
let teacherContext;
let teacherPage;
let tempMaterialId = null;
let tempFilePath = null;
let tempMaterialCleanup = 'not attempted';

function check(name, passed, detail) {
  checks.push({ name, result: passed ? 'PASS' : 'FAIL', detail: String(detail || '').replace(/\|/g, '\\|').replace(/[\r\n]+/g, ' ') });
}
function note(name, state, detail) {
  checks.push({ name, result: state, detail: String(detail || '').replace(/\|/g, '\\|').replace(/[\r\n]+/g, ' ') });
}
function instrument(page, label) {
  page.on('pageerror', error => runtimeErrors.push({ label, type: 'pageerror', message: error.message }));
  page.on('console', message => {
    if (message.type() !== 'error') return;
    const text = message.text();
    if (/Content Security Policy|Refused to (load|frame|execute)|violat(ed|ion).*policy/i.test(text)) {
      cspViolations.push({ label, message: text.slice(0, 350) });
    } else if (/Uncaught|Uncaught \(in promise\)/i.test(text)) {
      runtimeErrors.push({ label, type: 'console', message: text.slice(0, 350) });
    }
  });
  page.on('response', response => {
    if (/meet\.jit\.si\/external_api\.js/.test(response.url())) {
      jitsiApiResponses.push({ status: response.status(), url: response.url() });
    }
  });
  page.evaluateOnNewDocument(() => {
    window.__phase14CspViolations = [];
    window.addEventListener('securitypolicyviolation', event => {
      window.__phase14CspViolations.push({
        directive: event.violatedDirective,
        blockedURI: event.blockedURI,
        disposition: event.disposition
      });
    });
  });
  page.on('dialog', dialog => dialog.accept().catch(() => {}));
}
async function getPage(context, label) {
  const page = await context.newPage();
  page.setDefaultTimeout(12000);
  page.setDefaultNavigationTimeout(20000);
  instrument(page, label);
  return page;
}
async function navigateCheck(page, role, route, expectedStatus = 200) {
  const url = base + route;
  try {
    const response = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 20000 });
    const status = response ? response.status() : 0;
    const finalUrl = new URL(page.url());
    const ok = status === expectedStatus && finalUrl.origin === new URL(base).origin;
    check(role + ' page ' + route, ok, 'HTTP ' + status + '; final ' + finalUrl.pathname + finalUrl.search);
    if (status >= 500) runtimeErrors.push({ label: role + ' ' + route, type: 'HTTP', message: 'HTTP ' + status });
    return { status: status, page: page, response: response };
  } catch (error) {
    check(role + ' page ' + route, false, error.message);
    return { status: 0, page: page, response: null };
  }
}
async function login(context, role, email, password) {
  const page = await getPage(context, role + ' login');
  const r = await page.goto(base + '/login', { waitUntil: 'domcontentloaded' });
  if (!r || r.status() !== 200) throw new Error(role + ': login page did not return HTTP 200');
  await page.locator('input[name="email"]').fill(email);
  await page.locator('input[name="password"]').fill(password);
  await Promise.all([
    page.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 20000 }),
    page.locator('form[action$="/login"] button[type="submit"]').click()
  ]);
  const finalPath = new URL(page.url()).pathname;
  const expected = role === 'teacher' ? '/teacher/dashboard' : role === 'student' ? '/student/dashboard' : '/admin/dashboard';
  check(role + ' browser sign-in', finalPath === '/learnloop' + expected, 'final path ' + finalPath);
  if (finalPath !== '/learnloop' + expected) throw new Error(role + ': sign-in did not reach expected dashboard (' + finalPath + ')');
  const cookies = await context.cookies(base);
  check(role + ' session cookie is HttpOnly/SameSite',
    cookies.some(c => c.name === 'JSESSIONID' && c.httpOnly && String(c.sameSite).toLowerCase() === 'lax'),
    'JSESSIONID present=' + cookies.some(c => c.name === 'JSESSIONID') + '; HttpOnly and SameSite=Lax inspected');
  return page;
}
async function pageTextContains(page, text) {
  return page.locator('body').innerText().then(value => value.includes(text));
}
async function cleanupMaterial() {
  if (!teacherPage || !materialTitle) return;
  try {
    await teacherPage.goto(base + '/teacher/classes/2?tab=materials', { waitUntil: 'domcontentloaded', timeout: 15000 });
    const state = await teacherPage.evaluate(title => {
      const card = [...document.querySelectorAll('article.ll-material-manage-card')]
        .find(element => element.querySelector('h3') && element.querySelector('h3').textContent.trim() === title);
      if (!card) return { found: false };
      const form = [...card.querySelectorAll('form')]
        .find(element => element.querySelector('input[name="action"]') && element.querySelector('input[name="action"]').value === 'material-delete');
      if (!form) return { found: true, hasDelete: false };
      form.requestSubmit();
      return { found: true, hasDelete: true };
    }, materialTitle);
    if (!state.found) {
      tempMaterialCleanup = 'temporary material absent';
      return;
    }
    if (!state.hasDelete) throw new Error('Temporary material was found but no delete form was present.');
    await teacherPage.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 15000 }).catch(() => null);
    await teacherPage.goto(base + '/teacher/classes/2?tab=materials', { waitUntil: 'domcontentloaded', timeout: 15000 });
    const remains = await teacherPage.evaluate(title =>
      [...document.querySelectorAll('article.ll-material-manage-card')]
        .some(element => element.querySelector('h3') && element.querySelector('h3').textContent.trim() === title), materialTitle);
    if (remains) throw new Error('The temporary material still appears after deletion.');
    tempMaterialCleanup = 'deleted through teacher workspace and verified absent';
  } catch (error) {
    tempMaterialCleanup = 'cleanup through workspace failed: ' + error.message;
  }
}

async function run() {
  if (![teacherEmail, teacherPassword, materialTitle].every(Boolean)) {
    throw new Error('Missing phase 14 browser-test environment variables.');
  }
  fs.mkdirSync(path.dirname(reportPath), { recursive: true });
  tempFilePath = path.join(os.tmpdir(), 'learnloop-phase14-' + Date.now() + '.txt');
  const marker = 'LearnLoop Phase 14 file-download probe ' + Date.now() + ' ' + Math.random().toString(16).slice(2);
  fs.writeFileSync(tempFilePath, marker + '\r\n', { encoding: 'utf8', flag: 'wx' });

  browser = await puppeteer.launch({
    headless: true,
    executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    args: ['--no-first-run', '--no-default-browser-check', '--disable-background-networking']
  });

  const publicContext = await browser.createBrowserContext();
  const publicPage = await getPage(publicContext, 'public');
  await navigateCheck(publicPage, 'public', '/');
  try {
    await publicPage.waitForFunction(() => {
      const status = document.querySelector('#platform-status [data-status-text]');
      return status && status.textContent.trim() === 'Platform online';
    }, { timeout: 8000 });
    check('Landing platform-status pill updates', true, 'Platform online appeared after the health request');
  } catch {
    const status = await publicPage.$eval('#platform-status [data-status-text]', el => el.textContent.trim()).catch(() => 'status element missing');
    check('Landing platform-status pill updates', false, status);
  }
  try {
    await publicPage.locator('a[href="#features"]').click();
    await publicPage.waitForFunction(() => {
      const target = document.querySelector('#features');
      if (!target) return false;
      const rect = target.getBoundingClientRect();
      return rect.top < window.innerHeight && rect.bottom > 0;
    }, { timeout: 5000 });
    const nav = await publicPage.evaluate(() => {
      const target = document.querySelector('#features');
      return {
        hash: window.location.hash,
        targetExists: Boolean(target),
        inViewport: target ? target.getBoundingClientRect().top < window.innerHeight && target.getBoundingClientRect().bottom > 0 : false
      };
    });
    check('Landing page section anchor scrolls', nav.hash === '#features' && nav.targetExists && nav.inViewport,
      'hash=' + nav.hash + '; target=' + nav.targetExists + '; visible=' + nav.inViewport);
  } catch (error) {
    check('Landing page section anchor scrolls', false, error.message);
  }

  teacherContext = await browser.createBrowserContext();
  teacherPage = await login(teacherContext, 'teacher', teacherEmail, teacherPassword);
  for (const route of [
    '/teacher/dashboard', '/teacher/classes', '/teacher/classes/2',
    '/teacher/classes/2?tab=overview', '/teacher/classes/2?tab=materials',
    '/teacher/classes/2?tab=assessments', '/teacher/classes/2?tab=live',
    '/teacher/requests', '/teacher/materials', '/teacher/assessments',
    '/teacher/assessments/7', '/teacher/progress'
  ]) await navigateCheck(teacherPage, 'teacher', route);

  const jitsiResult = await navigateCheck(teacherPage, 'teacher', '/meetings/2');
  if (jitsiResult.status === 200) {
    await new Promise(resolve => setTimeout(resolve, 12000));
    const meeting = await teacherPage.evaluate(() => ({
      title: document.title,
      iframes: [...document.querySelectorAll('iframe')].map(frame => ({ src: frame.src, title: frame.title })),
      csp: window.__phase14CspViolations || []
    }));
    const jitsiFrame = meeting.iframes.find(frame => /meet\.jit\.si/i.test(frame.src));
    if (jitsiFrame) check('Jitsi iframe is present in protected meeting page', true, 'iframe title=' + (jitsiFrame.title || '(provider default)'));
    else if (jitsiApiResponses.some(response => response.status >= 200 && response.status < 400)) {
      note('Jitsi iframe initialization', 'BLOCKED', 'external_api.js responded but no Jitsi iframe appeared within 12 seconds');
    } else {
      note('Jitsi provider browser initialization', 'BLOCKED', 'External Jitsi API did not return a successful response from this browser session.');
    }
    const pageCsp = meeting.csp.length;
    const consoleCsp = cspViolations.filter(item => item.label.includes('teacher')).length;
    check('Jitsi page CSP has no observed violations', pageCsp === 0 && consoleCsp === 0,
      'securitypolicyviolation events=' + pageCsp + '; CSP console messages=' + consoleCsp + '; details=' + JSON.stringify(meeting.csp).slice(0, 500));
  }

  try {
    await teacherPage.goto(base + '/teacher/classes/2?tab=materials', { waitUntil: 'domcontentloaded' });
    await teacherPage.evaluate(() => {
      const details = [...document.querySelectorAll('details.ll-material-create')]
        .find(element => element.innerText.includes('Upload a file'));
      if (!details) throw new Error('The upload form was not rendered.');
      details.open = true;
    });
    await teacherPage.locator('#newFileTitle').fill(materialTitle);
    await teacherPage.locator('#newFileDescription').fill('Temporary Phase 14 download verification; delete after testing.');
    const fileInput = await teacherPage.$('#newFile');
    if (!fileInput) throw new Error('The upload file input was not found.');
    await fileInput.uploadFile(tempFilePath);
    await Promise.all([
      teacherPage.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 20000 }),
      teacherPage.locator('form[enctype="multipart/form-data"] button[type="submit"]').click()
    ]);
    check('Teacher can upload disposable allowlisted file',
      new URL(teacherPage.url()).pathname === '/learnloop/teacher/classes/2', 'final URL ' + teacherPage.url());
    const cardData = await teacherPage.evaluate(title => {
      const card = [...document.querySelectorAll('article.ll-material-manage-card')]
        .find(element => element.querySelector('h3') && element.querySelector('h3').textContent.trim() === title);
      if (!card) return null;
      const link = card.querySelector('a[href*="/materials/"]');
      return { href: link ? link.getAttribute('href') : null, text: card.innerText };
    }, materialTitle);
    if (!cardData || !cardData.href) throw new Error('Uploaded material card or material ID was not found.');
    const idMatch = cardData.href.match(/\/materials\/(\d+)/);
    if (!idMatch) throw new Error('Material details link did not contain an ID: ' + cardData.href);
    tempMaterialId = Number(idMatch[1]);
    const publishNavigation = teacherPage.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 15000 });
    const published = await teacherPage.evaluate(title => {
      const card = [...document.querySelectorAll('article.ll-material-manage-card')]
        .find(element => element.querySelector('h3') && element.querySelector('h3').textContent.trim() === title);
      const form = card && [...card.querySelectorAll('form')]
        .find(element => element.querySelector('input[name="action"]') && element.querySelector('input[name="action"]').value === 'material-publish');
      if (!form) return false;
      form.requestSubmit();
      return true;
    }, materialTitle);
    await publishNavigation;
    check('Teacher can publish temporary material', published, 'material id=' + tempMaterialId);
  } catch (error) {
    check('Teacher file upload/publish workflow', false, error.message);
  }

  if (tempMaterialId) {
    try {
      const download = await teacherPage.evaluate(async id => {
        const response = await fetch('/learnloop/materials/' + id + '/download', { cache: 'no-store' });
        return {
          status: response.status,
          type: response.headers.get('content-type'),
          disposition: response.headers.get('content-disposition'),
          nosniff: response.headers.get('x-content-type-options'),
          body: await response.text()
        };
      }, tempMaterialId);
      check('Class owner can download uploaded file',
        download.status === 200 && /text\/plain/i.test(download.type || '') &&
        /attachment/i.test(download.disposition || '') &&
        String(download.nosniff).toLowerCase() === 'nosniff' &&
        download.body.includes(marker),
        'HTTP ' + download.status + '; type=' + download.type + '; attachment=' + /attachment/i.test(download.disposition || '') +
        '; nosniff=' + download.nosniff + '; content-match=' + download.body.includes(marker));

      const anonymousContext = await browser.createBrowserContext();
      try {
        const anonymousPage = await getPage(anonymousContext, 'anonymous-download');
        await anonymousPage.goto(base + '/login', { waitUntil: 'domcontentloaded' });
        const anonymousResult = await anonymousPage.evaluate(async id => {
          const response = await fetch('/learnloop/materials/' + id + '/download', { cache: 'no-store' });
          return { status: response.status, redirected: response.redirected, url: response.url };
        }, tempMaterialId);
        const redirectPath = new URL(anonymousResult.url).pathname;
        const denied = anonymousResult.status === 403 || anonymousResult.status === 404 ||
          (anonymousResult.redirected && redirectPath === '/learnloop/login');
        check('Anonymous user cannot download file', denied,
          'HTTP ' + anonymousResult.status + '; redirected=' + anonymousResult.redirected + '; final path=' + redirectPath);
      } finally {
        await anonymousContext.close();
      }
    } catch (error) {
      check('Authorized file download workflow', false, error.message);
    }
  } else {
    note('Authorized file download', 'BLOCKED', 'No temporary file material ID was created, so file download assertions could not run.');
  }

  if (adminEmail && adminPassword) {
    const adminContext = await browser.createBrowserContext();
    try {
      const adminPage = await login(adminContext, 'admin', adminEmail, adminPassword);
      for (const route of [
        '/admin/dashboard', '/admin/teachers', '/admin/subjects',
        '/admin/assignments', '/admin/users', '/admin/classes',
        '/admin/classes/2', '/admin/classes/2?tab=overview',
        '/admin/classes/2?tab=materials', '/admin/classes/2?tab=assessments',
        '/admin/classes/2?tab=live'
      ]) await navigateCheck(adminPage, 'admin', route);
    } catch (error) {
      check('Admin read-only workflow initialization', false, error.message);
    } finally {
      await adminContext.close();
    }
  } else {
    const adminReportPath = path.resolve(__dirname, '..', 'docs', 'phase14-admin-isolated-browser.md');
    const adminReport = fs.existsSync(adminReportPath) ? fs.readFileSync(adminReportPath, 'utf8') : '';
    const adminVerified = /15 passed, 0 failed/.test(adminReport);
    note('Signed-in admin classroom tabs (isolated browser suite)', adminVerified ? 'PASS' : 'BLOCKED',
      adminVerified ? '15/15 passed in the separate disposable-database browser suite; see docs/phase14-admin-isolated-browser.md'
        : 'No approved administrator test credentials were supplied and no passing isolated-admin report was found.');
  }

  check('No uncaught application JavaScript errors', runtimeErrors.length === 0,
    runtimeErrors.length ? JSON.stringify(runtimeErrors.slice(0, 12)).slice(0, 900) : 'no uncaught page errors or HTTP 5xx responses observed');

  const passed = checks.filter(item => item.result === 'PASS').length;
  const failed = checks.filter(item => item.result === 'FAIL').length;
  const blocked = checks.filter(item => item.result === 'BLOCKED').length;
  const report = [
    '# Phase 14 - Browser End-to-End Validation',
    '',
    '- Executed: ' + new Date().toISOString(),
    '- Base URL: ' + base,
    '- Results: **' + passed + ' passed, ' + failed + ' failed, ' + blocked + ' blocked/manual-follow-up**',
    '- Disposable material ID: ' + (tempMaterialId || 'not created'),
    '- Temporary material cleanup: ' + tempMaterialCleanup,
    '',
    '| Check | Result | Detail |',
    '|---|---|---|',
    ...checks.map(item => '| ' + item.name + ' | ' + item.result + ' | ' + item.detail + ' |'),
    '',
    '## CSP observations',
    '',
    cspViolations.length ? cspViolations.map(item => '- ' + item.label + ': ' + item.message).join('\n') : '- No CSP refusal messages were observed in browser console output.',
    '',
    'Jitsi external API responses:',
    ...(jitsiApiResponses.length ? jitsiApiResponses.map(item => '- HTTP ' + item.status + ': ' + item.url) : ['- No response from the external Jitsi API was observed.']),
    '',
    'This run uses isolated browser contexts. The only application data change is a uniquely named temporary material in class 2, removed through the teacher workspace in cleanup. Existing demo assessment, enrollment, and class records are not changed.'
  ].join('\n');
  fs.writeFileSync(reportPath, report + '\n', 'utf8');
  console.log('BROWSER_REPORT=' + reportPath);
  console.log('BROWSER_TOTAL=' + checks.length + ' PASS=' + passed + ' FAIL=' + failed + ' BLOCKED=' + blocked);
  console.log('TEMP_MATERIAL_ID=' + (tempMaterialId || ''));
  console.log('TEMP_MATERIAL_CLEANUP=' + tempMaterialCleanup);
  if (failed > 0) process.exitCode = 1;
}

(async () => {
  try {
    await run();
  } catch (error) {
    console.error('Browser validation fatal error: ' + (error.stack || error.message));
    process.exitCode = 2;
  } finally {
    await cleanupMaterial();
    if (tempFilePath) {
      try { fs.unlinkSync(tempFilePath); } catch {}
    }
    if (browser) await browser.close().catch(() => {});
    try {
      if (fs.existsSync(reportPath)) {
        let report = fs.readFileSync(reportPath, 'utf8');
        report = report.replace(/- Temporary material cleanup: .*/, '- Temporary material cleanup: ' + tempMaterialCleanup);
        fs.writeFileSync(reportPath, report, 'utf8');
      }
    } catch {}
  }
})();
