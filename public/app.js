const out = document.getElementById('out');

async function show(promise) {
  try {
    const res = await promise;
    const text = await res.text();
    out.textContent = res.status + ' ' + res.statusText + '\n' + text;
  } catch (e) {
    out.textContent = 'Error: ' + e;
  }
}

document.getElementById('hello').onclick = () =>
  show(fetch('/api/hello?name=' + encodeURIComponent(document.getElementById('name').value)));
document.getElementById('stats').onclick = () => show(fetch('/api/stats'));
document.getElementById('echo').onclick = () =>
  show(fetch('/api/echo', { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: 'Hello from the browser' }));
