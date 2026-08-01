/* ============================================================
 *  20260801_100000_Create_Tb_Defs.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Defs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Defs
    (
        Id   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Defs PRIMARY KEY,
        Kind NVARCHAR(50)      NOT NULL DEFAULT (N''),
        Name NVARCHAR(200)     NOT NULL DEFAULT (N''),
        X1   BIGINT            NOT NULL DEFAULT (0),
        X2   BIGINT            NOT NULL DEFAULT (0),
        X3   NVARCHAR(300)     NOT NULL DEFAULT (N''),
        Cts  BIGINT            NOT NULL DEFAULT (0)
    );
    CREATE INDEX IX_Defs_Kind ON dbo.Defs(Kind);
END

GO
