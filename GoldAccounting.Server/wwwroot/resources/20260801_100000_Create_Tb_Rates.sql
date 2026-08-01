/* ============================================================
 *  20260801_100000_Create_Tb_Rates.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Rates', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Rates
    (
        Id      INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Rates PRIMARY KEY,
        Ts      BIGINT            NOT NULL DEFAULT (0),
        DateJ   NVARCHAR(20)      NOT NULL DEFAULT (N''),
        RateVal BIGINT            NOT NULL DEFAULT (0)
    );
    CREATE INDEX IX_Rates_Ts ON dbo.Rates(Ts DESC);
END

GO
