/* ============================================================
 *  20260801_100000_Create_Tb_DocRows.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.DocRows', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DocRows
    (
        Id    INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_DocRows PRIMARY KEY,
        DocId INT               NOT NULL DEFAULT (0),
        Seq   INT               NOT NULL DEFAULT (0),
        Txt   NVARCHAR(1000)    NOT NULL DEFAULT (N'')
    );
    CREATE INDEX IX_DocRows_DocId ON dbo.DocRows(DocId);
END

GO
