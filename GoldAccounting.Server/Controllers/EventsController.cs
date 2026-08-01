using Microsoft.AspNetCore.Mvc;
using GoldAccounting.Server.Data;
using GoldAccounting.Server.Services;

namespace GoldAccounting.Server.Controllers
{
    /// <summary>
    /// دریافت رویدادهای رخ‌داده در نسخهٔ ویندوز (ورود/خروج کاربر، ثبت اسناد و…)
    /// نسخهٔ ویندوز این‌ها را POST می‌کند تا در جدول Events ثبت و در صفحهٔ /events دیده شوند.
    /// </summary>
    [ApiController]
    [Route("api/[controller]")]
    public class EventsController : ControllerBase
    {
        private readonly AppDbContext _db;
        private readonly EventService _events;

        public EventsController(AppDbContext db, EventService events)
        {
            _db = db;
            _events = events;
        }

        [HttpPost]
        public IActionResult Post([FromBody] EventIn model)
        {
            if (model == null || string.IsNullOrEmpty(model.Action))
                return BadRequest(new { status = "error", message = "action الزامی است." });

            _events.Log(model.Action, model.TargetType ?? "WinApp", model.TargetId ?? 0, model.Details ?? "", model.Actor, model.BranchId ?? 0);
            return Ok(new { status = "success", message = "رویداد ثبت شد ✓" });
        }

        [HttpGet]
        public IActionResult Get(int? take)
        {
            var list = _db.Events.OrderByDescending(e => e.Ts).Take(take ?? 100).ToList();
            return Ok(list);
        }

        public class EventIn
        {
            public string Action { get; set; } = "";
            public string? Actor { get; set; }
            public string? TargetType { get; set; }
            public int? TargetId { get; set; }
            public string? Details { get; set; }
            public int? BranchId { get; set; }
        }
    }
}
