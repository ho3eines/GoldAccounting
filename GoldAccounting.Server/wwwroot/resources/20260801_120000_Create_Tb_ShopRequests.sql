/* ============================================================
 *  20260801_120000_Create_Tb_ShopRequests.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  درخواست‌های خرید از فروشگاه آنلاین
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 * ============================================================ */

IF OBJECT_ID(N'dbo.ShopRequests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ShopRequests
    (
        Id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ShopRequests PRIMARY KEY,
        ItemId       INT               NOT NULL DEFAULT (0),
        ItemName     NVARCHAR(300)     NOT NULL DEFAULT (N''),
        CustomerName NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Phone        NVARCHAR(50)      NOT NULL DEFAULT (N''),
        Qty          INT               NOT NULL DEFAULT (1),
        Note         NVARCHAR(500)     NOT NULL DEFAULT (N''),
        Status       NVARCHAR(20)      NOT NULL DEFAULT (N'new'),
        Cts          BIGINT            NOT NULL DEFAULT (0),
        DateJ        NVARCHAR(20)      NOT NULL DEFAULT (N'')
    );
    CREATE INDEX IX_ShopRequests_Status ON dbo.ShopRequests(Status);
    CREATE INDEX IX_ShopRequests_ItemId ON dbo.ShopRequests(ItemId);
END

GO
