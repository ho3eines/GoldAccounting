/* ============================================================
 *  20260801_100000_Create_Tb_BankTransactions.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.BankTransactions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BankTransactions
    (
        Id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_BankTransactions PRIMARY KEY,
        Ts           BIGINT            NOT NULL DEFAULT (0),
        DateJ        NVARCHAR(20)      NOT NULL DEFAULT (N''),
        BankId       INT               NOT NULL DEFAULT (0),
        Kind         NVARCHAR(10)      NOT NULL DEFAULT (N'in'),
        Amount       BIGINT            NOT NULL DEFAULT (0),
        Descr        NVARCHAR(500)     NOT NULL DEFAULT (N''),
        Cid          INT               NOT NULL DEFAULT (0),
        DocId        INT               NOT NULL DEFAULT (0),
        TransferCode NVARCHAR(100)     NOT NULL DEFAULT (N'')
    );
    CREATE INDEX IX_BankTransactions_BankId ON dbo.BankTransactions(BankId);
END

GO
