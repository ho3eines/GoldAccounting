/* ============================================================
 *  20260801_100000_Create_Tb_UserAccounts.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.UserAccounts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.UserAccounts
    (
        Id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_UserAccounts PRIMARY KEY,
        Username NVARCHAR(100)     NOT NULL DEFAULT (N''),
        Password NVARCHAR(200)     NOT NULL DEFAULT (N''),
        FullName NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Role     NVARCHAR(50)      NOT NULL DEFAULT (N'Admin'),
        Token    NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Cts      BIGINT            NOT NULL DEFAULT (0)
    );
    CREATE UNIQUE INDEX UX_UserAccounts_Username ON dbo.UserAccounts(Username);
END

GO
