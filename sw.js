const CACHE_NAME = 'tung-kinh-cache-v6'; // Đã bump version
const urlsToCache = [
  './',
  './index.html',
  './data.js',
  './manifest.json',
  './icon-192.png',
  './icon-512.png',
  './qrcode.png'
];

self.addEventListener('install', event => {
  self.skipWaiting(); 
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(urlsToCache))
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(self.clients.claim()); 
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.filter(name => name !== CACHE_NAME).map(name => caches.delete(name))
      );
    })
  );
});

// Chiến lược Stale-While-Revalidate (Lấy từ Cache trước, ngầm cập nhật từ Network)
self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request).then(cachedResponse => {
      const fetchPromise = fetch(event.request).then(networkResponse => {
        // Chỉ lưu vào cache nếu request thành công và là dữ liệu basic
        if (networkResponse && networkResponse.status === 200 && networkResponse.type === 'basic') {
          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, responseToCache));
        }
        return networkResponse;
      }).catch(() => {
        // Fallback an toàn khi offline hoàn toàn
        if (event.request.mode === 'navigate') {
          return caches.match('./index.html');
        }
        return new Response('Network error', { status: 408, headers: { 'Content-Type': 'text/plain' } });
      });
      
      // Trả về cache ngay lập tức nếu có, nếu không thì chờ Network
      return cachedResponse || fetchPromise;
    })
  );
});