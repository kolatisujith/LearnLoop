(function () {
    'use strict';

    var root = document.getElementById('meeting-app');
    var container = document.getElementById('jitsi-container');
    var status = document.getElementById('meeting-status');
    var api = null;

    function showUnavailable() {
        if (status) {
            status.textContent = 'The video service could not be loaded. Check your internet connection and reload this page.';
        }
    }

    if (!root || !container || !status || !window.JitsiMeetExternalAPI) {
        showUnavailable();
        return;
    }

    try {
        api = new window.JitsiMeetExternalAPI('meet.jit.si', {
            roomName: root.dataset.roomName,
            parentNode: container,
            width: '100%',
            height: '100%',
            userInfo: { displayName: root.dataset.displayName },
            configOverwrite: { prejoinPageEnabled: true },
            interfaceConfigOverwrite: { SHOW_JITSI_WATERMARK: true }
        });

        api.addListener('videoConferenceJoined', function () {
            status.textContent = 'Connected. Your microphone and camera are controlled by the meeting toolbar.';
        });
        api.addListener('videoConferenceLeft', function () {
            status.textContent = 'You left the live class. You can return to the classroom using the button above.';
        });
        api.addListener('readyToClose', function () {
            if (api) {
                api.dispose();
                api = null;
            }
            window.location.assign(root.dataset.returnUrl);
        });
        api.addListener('cameraError', function () {
            status.textContent = 'The meeting loaded, but camera access failed. Check your browser camera permissions.';
        });
    } catch (error) {
        showUnavailable();
    }

    window.addEventListener('beforeunload', function () {
        if (api) {
            api.dispose();
            api = null;
        }
    });
}());
