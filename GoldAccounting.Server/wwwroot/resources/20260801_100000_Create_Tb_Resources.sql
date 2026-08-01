/* ============================================================
 *  20260801_100000_Create_Tb_Resources.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Resources', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Resources
    (
        Id          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Resources PRIMARY KEY,
        FileName    NVARCHAR(300)     NOT NULL,
        Success     BIT               NOT NULL CONSTRAINT DF_Resources_Success DEFAULT (0),
        ExecuteTime DATETIME2         NOT NULL CONSTRAINT DF_Resources_ExecuteTime DEFAULT (SYSUTCDATETIME()),
        ExecuteMs   INT               NOT NULL CONSTRAINT DF_Resources_ExecuteMs DEFAULT (0),
        ErrorText   NVARCHAR(MAX)     NULL
    );
    CREATE UNIQUE INDEX UX_Resources_FileName ON dbo.Resources(FileName);
END

GO
