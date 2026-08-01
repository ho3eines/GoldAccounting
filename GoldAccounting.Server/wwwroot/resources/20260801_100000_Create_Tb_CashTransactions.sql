/* ============================================================
 *  20260801_100000_Create_Tb_CashTransactions.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.CashTransactions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CashTransactions
    (
        Id     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CashTransactions PRIMARY KEY,
        Ts     BIGINT            NOT NULL DEFAULT (0),
        DateJ  NVARCHAR(20)      NOT NULL DEFAULT (N''),
        Kind   NVARCHAR(10)      NOT NULL DEFAULT (N'in'),
        Amount BIGINT            NOT NULL DEFAULT (0),
        Descr  NVARCHAR(500)     NOT NULL DEFAULT (N''),
        Iid    INT               NOT NULL DEFAULT (0)
    );
    CREATE INDEX IX_CashTransactions_Ts ON dbo.CashTransactions(Ts DESC);
END

GO
