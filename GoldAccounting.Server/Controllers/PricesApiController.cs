using Microsoft.AspNetCore.Mvc;
using GoldAccounting.Server.Data;

namespace GoldAccounting.Server.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class PricesController : ControllerBase
    {
        private readonly AppDbContext _db;

        public PricesController(AppDbContext db)
        {
            _db = db;
        }

        [HttpGet]
        public IActionResult GetPrices()
        {
            var prices = _db.MarketPrices.ToList();
            return Ok(new { status = "success", prices });
        }

        [HttpPost("update")]
        public IActionResult UpdatePrice([FromBody] MarketPrice model)
        {
            var existing = _db.MarketPrices.FirstOrDefault(p => p.Key == model.Key);
            if (existing != null)
            {
                existing.Val = model.Val;
                existing.Ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            }
            else
            {
                model.Ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                _db.MarketPrices.Add(model);
            }
            _db.SaveChanges();
            return Ok(new { status = "success", message = "قیمت به‌روز شد" });
        }
    }
}
