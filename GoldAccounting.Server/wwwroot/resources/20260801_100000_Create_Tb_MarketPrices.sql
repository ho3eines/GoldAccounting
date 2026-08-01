/* ============================================================
 *  20260801_100000_Create_Tb_MarketPrices.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.MarketPrices', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MarketPrices
    (
        Id  INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MarketPrices PRIMARY KEY,
        Ts  BIGINT            NOT NULL DEFAULT (0),
        [Key] NVARCHAR(50)    NOT NULL DEFAULT (N''),
        Val BIGINT            NOT NULL DEFAULT (0)
    );
    CREATE INDEX IX_MarketPrices_Key ON dbo.MarketPrices([Key]);
END

GO
