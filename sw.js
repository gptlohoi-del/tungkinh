const CACHE_NAME = 'tung-kinh-cache-v3'; // Đã đổi sang v2 để bắt buộc trình duyệt làm mới cache
const urlsToCache = [
  './',
  './index.html',
  './data.js',           // Thêm file data.js vào danh sách cache
  './manifest.json',
  'https://cdn.tailwindcss.com',
  'https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js'
];

// Sự kiện cài đặt Service Worker và lưu trữ cache
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Đã mở cache');
        return cache.addAll(urlsToCache);
      })
  );
});

// Sự kiện chặn các request: Ưu tiên lấy từ mạng, nếu mất mạng thì lấy từ cache
self.addEventListener('fetch', event => {
  event.respondWith(
    fetch(event.request)
      .then(response => {
        // Nếu có mạng và tải file thành công, trả về file mới nhất từ mạng
        return response;
      })
      .catch(() => {
        // Nếu không có mạng (lỗi fetch), lôi file tương ứng đã lưu trong cache ra dùng
        return caches.match(event.request);
      })
  );
});

// Sự kiện kích hoạt và xóa cache cũ
self.addEventListener('activate', event => {
  const cacheWhitelist = [CACHE_NAME];
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheWhitelist.indexOf(cacheName) === -1) {
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});
