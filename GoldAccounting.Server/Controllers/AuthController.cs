using Microsoft.AspNetCore.Mvc;
using GoldAccounting.Server.Data;

namespace GoldAccounting.Server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly AppDbContext _db;

        public AuthController(AppDbContext db)
        {
            _db = db;
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
                return Unauthorized(new { status = "error", message = "نام کاربری یا رمز عبور نامعتبر است." });
            }

            if (string.IsNullOrEmpty(user.Token))
            {
                user.Token = "TOKEN-" + Guid.NewGuid().ToString();
                _db.SaveChanges();
            }

            return Ok(new {
                status = "success",
                token = user.Token,
                username = user.Username,
                fullName = user.FullName,
                role = user.Role,
                message = "ورود با موفقیت انجام شد ✓"
            });
        }
    }

    public class LoginRequest
    {
        public string Username { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
    }
}
