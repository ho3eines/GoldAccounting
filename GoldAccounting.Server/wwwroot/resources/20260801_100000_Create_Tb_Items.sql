/* ============================================================
 *  20260801_100000_Create_Tb_Items.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Items', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Items
    (
        Id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Items PRIMARY KEY,
        Code     BIGINT            NOT NULL DEFAULT (0),
        Name     NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Karat    INT               NOT NULL DEFAULT (750),
        Wmw      BIGINT            NOT NULL DEFAULT (0),
        WType    INT               NOT NULL DEFAULT (0),
        WVal     BIGINT            NOT NULL DEFAULT (0),
        StoneMw  BIGINT            NOT NULL DEFAULT (0),
        StoneVal BIGINT            NOT NULL DEFAULT (0),
        Descr    NVARCHAR(500)     NOT NULL DEFAULT (N''),
        Status   NVARCHAR(20)      NOT NULL DEFAULT (N'stock'),
        Cts      BIGINT            NOT NULL DEFAULT (0)
    );
    CREATE INDEX IX_Items_Status ON dbo.Items(Status);
END

GO
