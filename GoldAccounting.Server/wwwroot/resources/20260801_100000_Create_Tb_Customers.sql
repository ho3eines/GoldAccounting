/* ============================================================
 *  20260801_100000_Create_Tb_Customers.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Customers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Customers
    (
        Id      INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Customers PRIMARY KEY,
        Code    INT               NOT NULL DEFAULT (0),
        Name    NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Phone   NVARCHAR(50)      NOT NULL DEFAULT (N''),
        Grp     NVARCHAR(100)     NOT NULL DEFAULT (N''),
        Address NVARCHAR(300)     NOT NULL DEFAULT (N''),
        Note    NVARCHAR(500)     NOT NULL DEFAULT (N''),
        Cts     BIGINT            NOT NULL DEFAULT (0)
    );
    CREATE INDEX IX_Customers_Code ON dbo.Customers(Code);
END

GO
