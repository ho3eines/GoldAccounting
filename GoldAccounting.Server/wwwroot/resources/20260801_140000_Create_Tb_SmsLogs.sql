/* ============================================================
 *  20260801_140000_Create_Tb_SmsLogs.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 * ============================================================ */

IF OBJECT_ID(N'dbo.SmsLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SmsLogs
    (
        Id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SmsLogs PRIMARY KEY,
        Phone    NVARCHAR(50)      NOT NULL DEFAULT (N''),
        Kind     NVARCHAR(50)      NOT NULL DEFAULT (N''),
        Message  NVARCHAR(MAX)     NOT NULL DEFAULT (N''),
        Status   NVARCHAR(20)      NOT NULL DEFAULT (N'pending'),
        Error    NVARCHAR(500)     NOT NULL DEFAULT (N''),
        Ts       BIGINT            NOT NULL DEFAULT (0),
        DateJ    NVARCHAR(20)      NOT NULL DEFAULT (N'')
    );
    CREATE INDEX IX_SmsLogs_Phone ON dbo.SmsLogs(Phone);
END

GO
