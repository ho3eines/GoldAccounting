/* ============================================================
 *  20260801_100000_Create_Tb_CustomerTxs.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.CustomerTxs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CustomerTxs
    (
        Id     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CustomerTxs PRIMARY KEY,
        Cid    INT               NOT NULL DEFAULT (0),
        Ts     BIGINT            NOT NULL DEFAULT (0),
        DateJ  NVARCHAR(20)      NOT NULL DEFAULT (N''),
        Cash   BIGINT            NOT NULL DEFAULT (0),
        Goldmw BIGINT            NOT NULL DEFAULT (0),
        Descr  NVARCHAR(500)     NOT NULL DEFAULT (N''),
        Iid    BIGINT            NOT NULL DEFAULT (0)
    );
    CREATE INDEX IX_CustomerTxs_Cid ON dbo.CustomerTxs(Cid);
END

GO
