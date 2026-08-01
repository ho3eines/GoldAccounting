/* ============================================================
 *  20260801_140000_Create_Tb_Events.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Events', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Events
    (
        Id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Events PRIMARY KEY,
        Ts         BIGINT            NOT NULL DEFAULT (0),
        DateJ      NVARCHAR(20)      NOT NULL DEFAULT (N''),
        Actor      NVARCHAR(100)     NOT NULL DEFAULT (N''),
        Action     NVARCHAR(100)     NOT NULL DEFAULT (N''),
        TargetType NVARCHAR(50)      NOT NULL DEFAULT (N''),
        TargetId   INT               NOT NULL DEFAULT (0),
        Details    NVARCHAR(1000)    NOT NULL DEFAULT (N''),
        BranchId   INT               NOT NULL DEFAULT (0)
    );
    CREATE INDEX IX_Events_Ts ON dbo.Events(Ts DESC);
    CREATE INDEX IX_Events_Action ON dbo.Events(Action);
END

GO
