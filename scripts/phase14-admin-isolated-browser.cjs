'use strict';
const fs = require('fs');
const path = require('path');
const puppeteer = require('C:/Users/prath/AppData/Local/npm-cache/_npx/4b4c857f6efdfb61/node_modules/puppeteer');
const base = process.env.PHASE14_BASE_URL;
const email = process.env.PHASE14_ADMIN_EMAIL;
const password = process.env.PHASE14_ADMIN_PASSWORD;
const reportPath = path.resolve(__dirname, '..', 'docs', 'phase14-admin-isolated-browser.md');
const checks = [];
const errors = [];

function add(name, pass, detail) {
  checks.push({name, result: pass ? 'PASS' : 'FAIL', detail: String(detail || '').replace(/\|/g, '\\|').replace(/[\r\n]+/g, ' ')});
}

(async () => {
  let browser;
  try {
    if (![base, email, password].every(Boolean)) throw new Error('Test database browser environment is incomplete.');
    browser = await puppeteer.launch({
      headless: true,
      executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
      args: ['--no-first-run', '--no-default-browser-check', '--disable-background-networking']
    });
    const context = await browser.createBrowserContext();
    const page = await context.newPage();
    page.setDefaultTimeout(10000);
    page.setDefaultNavigationTimeout(15000);
    page.on('pageerror', error => errors.push(error.message));
    const loginPage = await page.goto(base + '/login', {waitUntil: 'domcontentloaded'});
    add('Admin login page', Boolean(loginPage) && loginPage.status() === 200, 'HTTP ' + (loginPage ? loginPage.status() : 0));
    await page.locator('input[name="email"]').fill(email);
    await page.locator('input[name="password"]').fill(password);
    await Promise.all([
      page.waitForNavigation({waitUntil: 'domcontentloaded'}),
      page.locator('form[action$="/login"] button[type="submit"]').click()
    ]);
    const expectedDashboard = new URL(base).pathname.replace(/\/$/, '') + '/admin/dashboard';
    add('Temporary admin sign-in', new URL(page.url()).pathname === expectedDashboard, 'final path ' + new URL(page.url()).pathname);
    const cookies = await context.cookies(base);
    add('Admin session cookie flags', cookies.some(c => c.name === 'JSESSIONID' && c.httpOnly && String(c.sameSite).toLowerCase() === 'lax'),
      'HttpOnly and SameSite=Lax checked');
    for (const route of [
      '/admin/dashboard', '/admin/teachers', '/admin/subjects',
      '/admin/assignments', '/admin/users', '/admin/classes',
      '/admin/classes/2', '/admin/classes/2?tab=overview',
      '/admin/classes/2?tab=materials', '/admin/classes/2?tab=assessments',
      '/admin/classes/2?tab=live'
    ]) {
      try {
        const response = await page.goto(base + route, {waitUntil: 'domcontentloaded'});
        const status = response ? response.status() : 0;
        add('Admin GET ' + route, status === 200, 'HTTP ' + status + '; final path ' + new URL(page.url()).pathname + new URL(page.url()).search);
      } catch (error) {
        add('Admin GET ' + route, false, error.message);
      }
    }
    add('No uncaught admin-page JavaScript errors', errors.length === 0, errors.length ? errors.slice(0, 8).join('; ') : 'none observed');
    await context.close();
  } catch (error) {
    add('Admin browser harness initialization', false, error.stack || error.message);
  } finally {
    if (browser) await browser.close().catch(() => {});
  }
  const passed = checks.filter(c => c.result === 'PASS').length;
  const failed = checks.filter(c => c.result === 'FAIL').length;
  fs.writeFileSync(reportPath, [
    '# Phase 14 - Isolated Admin Browser Validation',
    '',
    '- Base URL: ' + (base || '(not configured)'),
    '- Database: disposable MySQL test database only',
    '- Results: **' + passed + ' passed, ' + failed + ' failed**',
    '',
    '| Check | Result | Detail |',
    '|---|---|---|',
    ...checks.map(c => '| ' + c.name + ' | ' + c.result + ' | ' + c.detail + ' |'),
    '',
    'This report was generated against a temporary seeded database and a one-time-bootstrap administrator. It does not use or alter the existing learnloop_db service on port 3306.'
  ].join('\n') + '\n', 'utf8');
  console.log('ADMIN_BROWSER_REPORT=' + reportPath);
  console.log('ADMIN_BROWSER_TOTAL=' + checks.length + ' PASS=' + passed + ' FAIL=' + failed);
  if (failed) process.exitCode = 1;
})().catch(error => {
  console.error(error.stack || error.message);
  process.exitCode = 2;
});
