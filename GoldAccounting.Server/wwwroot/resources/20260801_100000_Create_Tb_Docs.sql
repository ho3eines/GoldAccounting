/* ============================================================
 *  20260801_100000_Create_Tb_Docs.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Docs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Docs
    (
        Id     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Docs PRIMARY KEY,
        Ts     BIGINT            NOT NULL DEFAULT (0),
        DateJ  NVARCHAR(20)      NOT NULL DEFAULT (N''),
        Descr  NVARCHAR(1000)    NOT NULL DEFAULT (N''),
        UpdTs  BIGINT            NOT NULL DEFAULT (0)
    );
    CREATE INDEX IX_Docs_DateJ ON dbo.Docs(DateJ);
END

GO
