.class public final Lcom/heytap/wearable/support/widget/j;
.super Ljava/lang/Object;
.source "SourceFile"


# instance fields
.field public a:Landroid/os/linearmotorvibrator/LinearmotorVibrator;

.field public b:Landroid/os/HandlerThread;

.field public c:Landroid/os/Handler;

.field public d:Lcom/heytap/wearable/support/widget/i;


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iget-object v0, p0, Lcom/heytap/wearable/support/widget/j;->a:Landroid/os/linearmotorvibrator/LinearmotorVibrator;

    if-nez v0, :cond_0

    const-string v0, "linearmotor"

    invoke-virtual {p1, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/os/linearmotorvibrator/LinearmotorVibrator;

    iput-object p1, p0, Lcom/heytap/wearable/support/widget/j;->a:Landroid/os/linearmotorvibrator/LinearmotorVibrator;

    :cond_0
    iget-object p1, p0, Lcom/heytap/wearable/support/widget/j;->b:Landroid/os/HandlerThread;

    if-nez p1, :cond_1

    new-instance p1, Landroid/os/HandlerThread;

    const-string v0, "crown_vibrate"

    invoke-direct {p1, v0}, Landroid/os/HandlerThread;-><init>(Ljava/lang/String;)V

    iput-object p1, p0, Lcom/heytap/wearable/support/widget/j;->b:Landroid/os/HandlerThread;

    invoke-virtual {p1}, Ljava/lang/Thread;->start()V

    :cond_1
    iget-object p1, p0, Lcom/heytap/wearable/support/widget/j;->c:Landroid/os/Handler;

    if-nez p1, :cond_2

    iget-object p1, p0, Lcom/heytap/wearable/support/widget/j;->b:Landroid/os/HandlerThread;

    invoke-virtual {p1}, Landroid/os/HandlerThread;->getLooper()Landroid/os/Looper;

    move-result-object p1

    if-eqz p1, :cond_2

    new-instance p1, Landroid/os/Handler;

    iget-object v0, p0, Lcom/heytap/wearable/support/widget/j;->b:Landroid/os/HandlerThread;

    invoke-virtual {v0}, Landroid/os/HandlerThread;->getLooper()Landroid/os/Looper;

    move-result-object v0

    invoke-direct {p1, v0}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    iput-object p1, p0, Lcom/heytap/wearable/support/widget/j;->c:Landroid/os/Handler;

    :cond_2
    return-void
.end method


# virtual methods
.method public final a(I)V
    .locals 3

    iget-object v0, p0, Lcom/heytap/wearable/support/widget/j;->c:Landroid/os/Handler;

    new-instance v1, Lcom/heytap/wearable/support/widget/i;

    const/4 v2, 0x1

    invoke-direct {v1, p0, p1, v2}, Lcom/heytap/wearable/support/widget/i;-><init>(Lcom/heytap/wearable/support/widget/j;II)V

    invoke-virtual {v0, v1}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    return-void
.end method

.method public final b()V
    .locals 1

    iget-object v0, p0, Lcom/heytap/wearable/support/widget/j;->b:Landroid/os/HandlerThread;

    if-eqz v0, :cond_0

    invoke-virtual {v0}, Landroid/os/HandlerThread;->quitSafely()Z

    const/4 v0, 0x0

    iput-object v0, p0, Lcom/heytap/wearable/support/widget/j;->b:Landroid/os/HandlerThread;

    :cond_0
    return-void
.end method
