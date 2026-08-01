/* ============================================================
 *  20260801_100000_Create_Tb_AssetsLedger.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.AssetsLedger', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AssetsLedger
    (
        Id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_AssetsLedger PRIMARY KEY,
        DocId        INT               NOT NULL DEFAULT (0),
        Ts           BIGINT            NOT NULL DEFAULT (0),
        DateJ        NVARCHAR(20)      NOT NULL DEFAULT (N''),
        Scope        NVARCHAR(50)      NOT NULL DEFAULT (N''),
        Asset        NVARCHAR(100)     NOT NULL DEFAULT (N''),
        Qty          FLOAT             NOT NULL DEFAULT (0),
        Karat        INT               NOT NULL DEFAULT (0),
        Cid          INT               NOT NULL DEFAULT (0),
        Descr        NVARCHAR(500)     NOT NULL DEFAULT (N''),
        TransferCode NVARCHAR(100)     NOT NULL DEFAULT (N'')
    );
    CREATE INDEX IX_AssetsLedger_ScopeAsset ON dbo.AssetsLedger(Scope, Asset);
    CREATE INDEX IX_AssetsLedger_Cid ON dbo.AssetsLedger(Cid);
END

GO
