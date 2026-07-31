using System.ComponentModel.DataAnnotations;

namespace GoldAccounting.Server.Data
{
    public class UserAccount
    {
        [Key]
        public int Id { get; set; }
        public string Username { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty; // In production hashed, here clean/hashed for sync
        public string FullName { get; set; } = string.Empty;
        public string Role { get; set; } = "Admin"; // Admin / Cashier / Viewer
        public string Token { get; set; } = string.Empty; // Auth token for Android sign-in
        public long Cts { get; set; }
    }
}
