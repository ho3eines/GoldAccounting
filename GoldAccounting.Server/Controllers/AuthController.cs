using Microsoft.AspNetCore.Mvc;
using GoldAccounting.Server.Data;
using GoldAccounting.Server.Services;

namespace GoldAccounting.Server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly AppDbContext _db;
        private readonly EventService _events;

        public AuthController(AppDbContext db, EventService events)
        {
            _db = db;
            _events = events;
        }

        [HttpPost("login")]
        public IActionResult Login([FromBody] LoginRequest model)
        {
            if (model == null || string.IsNullOrEmpty(model.Username) || string.IsNullOrEmpty(model.Password))
            {
                return BadRequest(new { status = "error", message = "نام کاربری و رمز عبور الزامی است." });
            }

            var user = _db.UserAccounts.FirstOrDefault(u => u.Username == model.Username && u.Password == model.Password);
            if (user == null)
            {
                _events.LogLogin(model.Username, false, "api");
                return Unauthorized(new { status = "error", message = "نام کاربری یا رمز عبور نامعتبر است." });
            }

            if (string.IsNullOrEmpty(user.Token))
            {
                user.Token = "TOKEN-" + Guid.NewGuid().ToString();
                _db.SaveChanges();
            }

            _events.LogLogin(user.Username, true, "api");

            return Ok(new {
                status = "success",
                token = user.Token,
                username = user.Username,
                fullName = user.FullName,
                role = user.Role,
                message = "ورود با موفقیت انجام شد ✓"
            });
        }

        /// <summary>
        /// ورود نسخهٔ ویندوز با برنامهٔ موبایل — بدون قفل سخت‌افزاری:
        /// موبایل با نام کاربری/رمز وارد شده و یک کد ۶ رقمی یک‌بارمصرف (۵ دقیقه) دریافت می‌کند.
        /// </summary>
        [HttpPost("win-request")]
        public IActionResult WinRequest([FromBody] LoginRequest model)
        {
            if (model == null || string.IsNullOrEmpty(model.Username) || string.IsNullOrEmpty(model.Password))
                return BadRequest(new { status = "error", message = "نام کاربری و رمز عبور الزامی است." });

            var user = _db.UserAccounts.FirstOrDefault(u => u.Username == model.Username && u.Password == model.Password);
            if (user == null)
            {
                _events.LogLogin(model.Username, false, "win-request");
                return Unauthorized(new { status = "error", message = "نام کاربری یا رمز عبور نامعتبر است." });
            }

            user.LoginCode = Random.Shared.Next(100000, 999999).ToString();
            user.LoginCodeExp = DateTimeOffset.Now.ToUnixTimeMilliseconds() + 5 * 60 * 1000; // ۵ دقیقه
            _db.SaveChanges();

            _events.Log("win_code_requested", "User", user.Id, $"صدور کد ورود ویندوز برای {user.Username}", user.Username);

            return Ok(new { status = "success", code = user.LoginCode, message = "کد یک‌بارمصرف صادر شد (۵ دقیقه معتبر)" });
        }

        /// <summary>تأیید کد یک‌بارمصرف در نسخهٔ ویندوز و دریافت توکن نشست</summary>
        [HttpPost("win-verify")]
        public IActionResult WinVerify([FromBody] WinVerifyRequest model)
        {
            if (model == null || string.IsNullOrEmpty(model.Username) || string.IsNullOrEmpty(model.Code))
                return BadRequest(new { status = "error", message = "نام کاربری و کد الزامی است." });

            var user = _db.UserAccounts.FirstOrDefault(u => u.Username == model.Username);
            if (user == null)
                return Unauthorized(new { status = "error", message = "کاربر یافت نشد." });

            if (string.IsNullOrEmpty(user.LoginCode) || user.LoginCode != model.Code.Trim())
                return Unauthorized(new { status = "error", message = "کد نامعتبر است." });

            if (user.LoginCodeExp < DateTimeOffset.Now.ToUnixTimeMilliseconds())
                return Unauthorized(new { status = "error", message = "کد منقضی شده است. دوباره از موبایل کد بگیرید." });

            // کد یک‌بارمصرف — بلافاصله مصرف می‌شود
            user.LoginCode = "";
            user.LoginCodeExp = 0;
            if (string.IsNullOrEmpty(user.Token))
                user.Token = "TOKEN-" + Guid.NewGuid().ToString();
            _db.SaveChanges();

            _events.LogLogin(user.Username, true, "windows");

            return Ok(new {
                status = "success",
                token = user.Token,
                username = user.Username,
                fullName = user.FullName,
                role = user.Role,
                message = "ورود ویندوز با موفقیت انجام شد ✓ (بدون قفل سخت‌افزاری)"
            });
        }
    }

    public class LoginRequest
    {
        public string Username { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
    }

    public class WinVerifyRequest
    {
        public string Username { get; set; } = string.Empty;
        public string Code { get; set; } = string.Empty;
    }
}
