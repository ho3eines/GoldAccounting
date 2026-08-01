/* ============================================================
 *  20260801_100000_Create_Tb_InvoiceLines.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.InvoiceLines', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.InvoiceLines
    (
        Id     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_InvoiceLines PRIMARY KEY,
        Iid    INT               NOT NULL DEFAULT (0),
        ItemId INT               NOT NULL DEFAULT (0),
        Title  NVARCHAR(300)     NOT NULL DEFAULT (N''),
        Karat  INT               NOT NULL DEFAULT (750),
        Wmw    BIGINT            NOT NULL DEFAULT (0),
        Unit   BIGINT            NOT NULL DEFAULT (0),
        Wage   BIGINT            NOT NULL DEFAULT (0),
        Stone  BIGINT            NOT NULL DEFAULT (0),
        Tax    BIGINT            NOT NULL DEFAULT (0),
        Total  BIGINT            NOT NULL DEFAULT (0)
    );
    CREATE INDEX IX_InvoiceLines_Iid ON dbo.InvoiceLines(Iid);
END

GO
