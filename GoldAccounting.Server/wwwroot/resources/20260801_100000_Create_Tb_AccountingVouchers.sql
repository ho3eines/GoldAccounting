/* ============================================================
 *  20260801_100000_Create_Tb_AccountingVouchers.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.AccountingVouchers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AccountingVouchers
    (
        Id      INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_AccountingVouchers PRIMARY KEY,
        Ts      BIGINT            NOT NULL DEFAULT (0),
        DateJ   NVARCHAR(20)      NOT NULL DEFAULT (N''),
        Title   NVARCHAR(300)     NOT NULL DEFAULT (N''),
        RefType NVARCHAR(50)      NOT NULL DEFAULT (N''),
        RefId   INT               NOT NULL DEFAULT (0),
        Descr   NVARCHAR(1000)    NOT NULL DEFAULT (N'')
    );
END

GO
