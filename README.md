# KINH TỤNG — Android APK + cập nhật nội dung OTA qua GitHub Pages

Bộ mã nguồn này được tạo từ chính file ZIP bạn cung cấp. Nội dung kinh `data.js`, giao diện `index.html`, Service Worker `sw.js` và các ảnh gốc được giữ nguyên. Thư mục `android/` là ứng dụng Android (WebView) mới, dùng **icon gốc** và tên hiển thị **KINH TỤNG**.

## 1. Đưa lên GitHub

1. Tạo repository **Public** trên GitHub (hoặc dùng repo hiện tại). Nhánh mặc định nên là `main`.
2. Giải nén file ZIP này. **Đưa các tệp và thư mục bên trong thư mục `tungkinh-github-apk` lên thư mục gốc của repo**, đặc biệt phải có `.github/workflows/`, `android/`, `index.html`, `data.js`, `sw.js`, `manifest.json` và ba ảnh.
   - Để tải thư mục `.github` lên đầy đủ, dễ nhất dùng GitHub Desktop hoặc `git add . && git commit && git push`. Giao diện Upload files trên GitHub có thể hỗ trợ kéo-thả cả thư mục tuỳ trình duyệt.
3. Vào **Settings → Pages → Build and deployment → Source: GitHub Actions**.
4. Vào **Actions**, chọn **Cập nhật website KINH TỤNG (OTA)** → **Run workflow** nếu cần triển khai ngay (lần push đầu cũng sẽ tự chạy).
5. Mở trang Pages mà GitHub hiển thị trong Settings → Pages để xác nhận website hoạt động.

**Mặc định địa chỉ ứng dụng sẽ là** `https://TEN-TAI-KHOAN.github.io/TEN-REPO/`, được tính tự động theo repo thực tế lúc GitHub Actions build APK. Nếu Pages dùng **tên miền riêng** hoặc địa chỉ khác, vào **Settings → Secrets and variables → Actions → Variables**, thêm biến `PAGES_URL` bằng **địa chỉ HTTPS đầy đủ**, ví dụ `https://kinhtung.example/` (nhớ dấu `/` cuối), *trước khi build APK*.

## 2. Lấy APK không cần Release Key

1. Vào tab **Actions** → **Tạo APK KINH TỤNG (không Release Key)** → **Run workflow** → **Run workflow**.
2. Chờ workflow báo màu xanh. APK được tạo tự động từ Android SDK và ký bằng **debug key** của bản build (không nhập Release Key, không cần GitHub Secret).
3. Vào tab **Releases** của repo → chọn **KINH TỤNG — APK (bản debug)** → tải **KINH-TUNG.apk** rồi cài lên điện thoại Android. Hoặc vào workflow run → **Artifacts → KINH-TUNG-APK-debug** (artifact dạng ZIP có APK bên trong).
4. Nếu GitHub Actions báo `Resource not accessible by integration` ở bước Releases, kiểm tra **Settings → Actions → General → Workflow permissions**, cho phép workflow quyền ghi nội dung phù hợp với chính sách repository. APK vẫn có trong Artifacts khi bước build và upload thành công.

**Lưu ý rất quan trọng về chữ ký:** Mỗi GitHub Actions runner mới có thể tạo debug key khác nhau. Vì thế **APK build lần sau có thể không cập nhật đè lên APK build lần trước**; Android sẽ yêu cầu gỡ bản cũ trước (có thể mất dữ liệu ứng dụng). Điều này **không ảnh hưởng OTA nội dung**. Muốn cập nhật chính APK không mất dữ liệu về sau, cần một khóa ký ổn định và giữ an toàn; bạn đã chọn không sử dụng Release Key nên gói này chủ ý chỉ phục vụ thử nghiệm/cài cá nhân, không phát hành lên Play Store.

## 3. Cách OTA hoạt động (không phải OTA cập nhật file APK)

- Khi bạn thay đổi `data.js`, `index.html`, `sw.js` hoặc ảnh website và **push lên GitHub** thì workflow Pages tự chạy.
- Ứng dụng Android mở **cùng website** trên GitHub Pages. Service Worker sẵn có tải HTML và `data.js` theo chiến lược **network-first** khi có mạng, và giữ nội dung đã lưu để đọc khi offline sau lần mở online đầu tiên.
- Bạn **không phải build APK lại** chỉ vì sửa nội dung kinh, căn lề hay CSS trên website. Đóng/mở lại ứng dụng khi có mạng để nhận nội dung mới. Nếu ứng dụng vẫn hiển thị bản cũ, hãy mở lại hoặc tải lại trang khi có mạng.
- **OTA không tự nâng cấp mã Android** (`android/`), quyền ứng dụng hay biểu tượng app đã đóng trong APK. Những thay đổi này đòi hỏi build và cài APK mới (với chữ ký ổn định nếu muốn cài đè).
- Website dùng Tailwind và thư viện QR từ CDN: để đảm bảo ngoại tuyến, nên mở ứng dụng ít nhất một lần lúc có mạng. Lần mở offline đầu tiên có thể hiện màn hình yêu cầu kết nối mạng.

## 4. Phạm vi tính năng

- Hiển thị màn hình điện thoại dọc/ngang theo CSS responsive của `index.html` gốc, **không khóa xoay**.
- Giữ `localStorage` để lưu bộ kinh đã chọn và các tuỳ chọn đọc.
- Hỗ trợ nút **In tài liệu** của website bằng Android Print / Lưu PDF.
- Hỗ trợ tải mã QR dạng PNG vào thư mục Ảnh trên Android 10+ (máy cũ có thể lưu vào thư mục ảnh riêng của ứng dụng).
- Link ra ngoài website được mở bằng trình duyệt mặc định; các nội dung kinh vẫn hiển thị trong ứng dụng.

## 5. Cấu trúc

| Thành phần | Vai trò |
|---|---|
| `index.html`, `data.js`, `sw.js`, `manifest.json`, `*.png` | Website hiện có, không sửa nội dung |
| `.github/workflows/deploy-pages.yml` | Tự triển khai website lên GitHub Pages, phục vụ OTA |
| `.github/workflows/build-apk.yml` | Tạo APK debug và đăng vào GitHub Releases |
| `android/` | Mã nguồn ứng dụng Android tên KINH TỤNG |

### Build thủ công nếu có Android SDK & Gradle

```bash
gradle -p android :app:assembleDebug -PsiteUrl=https://TEN-TAI-KHOAN.github.io/TEN-REPO/
```

Kết quả: `android/app/build/outputs/apk/debug/app-debug.apk`.
