/* ============================================================
 *  20260801_100000_Create_Tb_Etikets.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Etikets', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Etikets
    (
        Id        INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Etikets PRIMARY KEY,
        Code      NVARCHAR(100)     NOT NULL DEFAULT (N''),
        Name      NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Wmw       BIGINT            NOT NULL DEFAULT (0),
        ItemId    INT               NOT NULL DEFAULT (0),
        Photo     NVARCHAR(300)     NOT NULL DEFAULT (N''),
        Mezane    NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Rfid      NVARCHAR(100)     NOT NULL DEFAULT (N''),
        UpdatedTs BIGINT            NOT NULL DEFAULT (0),
        Cts       BIGINT            NOT NULL DEFAULT (0),
        Status    NVARCHAR(20)      NOT NULL DEFAULT (N'stock')
    );
    CREATE INDEX IX_Etikets_Code ON dbo.Etikets(Code);
END

GO
