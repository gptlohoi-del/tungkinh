const CACHE_NAME = 'tung-kinh-cache-v5'; // Tăng version để trình duyệt cập nhật
const urlsToCache = [
  './',
  './index.html',
  './data.js',
  './manifest.json',
  './icon-192.png',   // Đã bổ sung
  './icon-512.png',   // Đã bổ sung
  './qrcode.png',     // Đã bổ sung
  'https://cdn.tailwindcss.com',
  'https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js'
];

// Sự kiện cài đặt: Lưu cache và ép phiên bản mới kích hoạt ngay
self.addEventListener('install', event => {
  self.skipWaiting(); // Bỏ qua trạng thái chờ (waiting)
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Đã mở cache v5');
        return cache.addAll(urlsToCache);
      })
  );
});

// Sự kiện kích hoạt: Xóa cache cũ và chiếm quyền điều khiển trang ngay
self.addEventListener('activate', event => {
  event.waitUntil(self.clients.claim()); 
  const cacheWhitelist = [CACHE_NAME];
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheWhitelist.indexOf(cacheName) === -1) {
            console.log('Đã xóa cache cũ:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});

// Sự kiện fetch: Chiến lược Network-First, fallback về Cache một cách an toàn
self.addEventListener('fetch', event => {
  event.respondWith(
    fetch(event.request)
      .catch(() => {
        // Nếu không có mạng (fetch lỗi), tìm trong cache
        return caches.match(event.request).then(response => {
          if (response) {
            return response; // Trả về file tìm thấy trong cache
          }
          // QUAN TRỌNG: Nếu đang mở trang (navigate) mà không tìm thấy URL chính xác,
          // thì tự động nạp file ./index.html từ cache.
          if (event.request.mode === 'navigate') {
            return caches.match('./index.html');
          }
          return undefined; // Trả về lỗi nếu không phải là điều hướng trang
        });
      })
  );
});
