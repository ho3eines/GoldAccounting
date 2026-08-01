/* ============================================================
 *  20260801_100000_Create_Tb_VoucherRows.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.VoucherRows', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.VoucherRows
    (
        Id          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_VoucherRows PRIMARY KEY,
        VoucherId   INT               NOT NULL DEFAULT (0),
        AccountCode NVARCHAR(50)      NOT NULL DEFAULT (N''),
        AccountName NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Debit       BIGINT            NOT NULL DEFAULT (0),
        Credit      BIGINT            NOT NULL DEFAULT (0),
        Descr       NVARCHAR(500)     NOT NULL DEFAULT (N'')
    );
    CREATE INDEX IX_VoucherRows_VoucherId ON dbo.VoucherRows(VoucherId);
END

GO
