/* ============================================================
 *  20260801_100000_Create_Tb_CheckTransactions.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.CheckTransactions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CheckTransactions
    (
        Id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CheckTransactions PRIMARY KEY,
        Ts           BIGINT            NOT NULL DEFAULT (0),
        DateJ        NVARCHAR(20)      NOT NULL DEFAULT (N''),
        DueJ         NVARCHAR(20)      NOT NULL DEFAULT (N''),
        Amount       BIGINT            NOT NULL DEFAULT (0),
        BankId       INT               NOT NULL DEFAULT (0),
        Cid          INT               NOT NULL DEFAULT (0),
        Cname        NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Kind         NVARCHAR(10)      NOT NULL DEFAULT (N'receive'),
        No           NVARCHAR(100)     NOT NULL DEFAULT (N''),
        Status       NVARCHAR(20)      NOT NULL DEFAULT (N'open'),
        Descr        NVARCHAR(500)     NOT NULL DEFAULT (N''),
        DocId        INT               NOT NULL DEFAULT (0),
        TransferCode NVARCHAR(100)     NOT NULL DEFAULT (N'')
    );
    CREATE INDEX IX_CheckTransactions_DocId ON dbo.CheckTransactions(DocId);
END

GO
