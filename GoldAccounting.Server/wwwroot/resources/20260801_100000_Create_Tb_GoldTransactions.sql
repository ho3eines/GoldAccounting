/* ============================================================
 *  20260801_100000_Create_Tb_GoldTransactions.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.GoldTransactions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.GoldTransactions
    (
        Id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_GoldTransactions PRIMARY KEY,
        Ts           BIGINT            NOT NULL DEFAULT (0),
        DateJ        NVARCHAR(20)      NOT NULL DEFAULT (N''),
        Kind         NVARCHAR(10)      NOT NULL DEFAULT (N'in'),
        Wmw          BIGINT            NOT NULL DEFAULT (0),
        Karat        INT               NOT NULL DEFAULT (750),
        Descr        NVARCHAR(500)     NOT NULL DEFAULT (N''),
        Cid          INT               NOT NULL DEFAULT (0),
        DocId        INT               NOT NULL DEFAULT (0),
        TransferCode NVARCHAR(100)     NOT NULL DEFAULT (N'')
    );
    CREATE INDEX IX_GoldTransactions_DocId ON dbo.GoldTransactions(DocId);
END

GO
