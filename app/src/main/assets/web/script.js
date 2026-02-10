// Refresh status setiap 3 detik
setInterval(refreshStatus, 3000);
setInterval(refreshDevices, 5000);

async function refreshStatus() {
    try {
        const response = await fetch('/api/status');
        const data = await response.json();
        updateStatusDisplay(data);
    } catch (error) {
        console.error('Error fetching status:', error);
    }
}

function updateStatusDisplay(data) {
    const statusDiv = document.getElementById('status');
    const html = `
        <div>
            <strong>WiFi Hotspot:</strong>
            <span class="${data.wifiHotspot ? 'success' : 'offline'}">
                ${data.wifiHotspot ? '🟢 Active' : '🔴 Inactive'}
            </span>
        </div>
        <div>
            <strong>USB Tethering:</strong>
            <span class="${data.usbTethering ? 'success' : 'offline'}">
                ${data.usbTethering ? '🟢 Active' : '🔴 Inactive'}
            </span>
        </div>
        <div>
            <strong>Bluetooth:</strong>
            <span class="${data.bluetoothTethering ? 'success' : 'offline'}">
                ${data.bluetoothTethering ? '🟢 Active' : '🔴 Inactive'}
            </span>
        </div>
        <div>
            <strong>Connected Devices:</strong> ${data.connectedDevices}
        </div>
        <div>
            <strong>IP Address:</strong> ${data.ipAddress}
        </div>
    `;
    statusDiv.innerHTML = html;
}

async function refreshDevices() {
    try {
        const response = await fetch('/api/devices');
        const data = await response.json();
        updateDevicesDisplay(data);
    } catch (error) {
        console.error('Error fetching devices:', error);
    }
}

function updateDevicesDisplay(data) {
    const devicesDiv = document.getElementById('devices');
    let html = '';
    
    if (data.devices.length === 0) {
        html = '<p>No devices connected</p>';
    } else {
        data.devices.forEach(device => {
            html += `<div class="device-item">${device.device}</div>`;
        });
    }
    
    devicesDiv.innerHTML = html;
}

// Event Listeners
document.getElementById('hotspotStart').addEventListener('click', async () => {
    const response = await fetch('/api/hotspot/start', { method: 'POST' });
    const data = await response.json();
    addLog(data.message, data.success);
    setTimeout(refreshStatus, 1000);
});

document.getElementById('hotspotStop').addEventListener('click', async () => {
    const response = await fetch('/api/hotspot/stop', { method: 'POST' });
    const data = await response.json();
    addLog(data.message, data.success);
    setTimeout(refreshStatus, 1000);
});

document.getElementById('usbStart').addEventListener('click', async () => {
    const response = await fetch('/api/usb/start', { method: 'POST' });
    const data = await response.json();
    addLog(data.message, data.success);
    setTimeout(refreshStatus, 1000);
});

document.getElementById('usbStop').addEventListener('click', async () => {
    const response = await fetch('/api/usb/stop', { method: 'POST' });
    const data = await response.json();
    addLog(data.message, data.success);
    setTimeout(refreshStatus, 1000);
});

function addLog(message, isSuccess = true) {
    const logsDiv = document.getElementById('logs');
    const now = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.className = `log-entry ${isSuccess ? 'success' : 'error'}`;
    entry.textContent = `[${now}] ${message}`;
    logsDiv.insertBefore(entry, logsDiv.firstChild);
    
    // Keep only last 50 logs
    while (logsDiv.children.length > 50) {
        logsDiv.removeChild(logsDiv.lastChild);
    }
}

// Initial load
refreshStatus();
refreshDevices();
