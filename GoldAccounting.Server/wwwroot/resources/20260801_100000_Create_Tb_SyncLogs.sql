/* ============================================================
 *  20260801_100000_Create_Tb_SyncLogs.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.SyncLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SyncLogs
    (
        Id             INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SyncLogs PRIMARY KEY,
        TransferCode   NVARCHAR(100)     NOT NULL DEFAULT (N''),
        DeviceId       NVARCHAR(100)     NOT NULL DEFAULT (N''),
        Ts             BIGINT            NOT NULL DEFAULT (0),
        PayloadSummary NVARCHAR(500)     NOT NULL DEFAULT (N''),
        Status         NVARCHAR(50)      NOT NULL DEFAULT (N'success'),
        Message        NVARCHAR(1000)    NOT NULL DEFAULT (N'')
    );
    CREATE UNIQUE INDEX UX_SyncLogs_TransferCode ON dbo.SyncLogs(TransferCode);
END

GO
