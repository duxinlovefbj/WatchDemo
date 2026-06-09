.class public final synthetic Lcom/heytap/wearable/support/widget/i;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# instance fields
.field public final synthetic a:I

.field public final synthetic b:Lcom/heytap/wearable/support/widget/j;

.field public final synthetic c:I


# direct methods
.method public synthetic constructor <init>(Lcom/heytap/wearable/support/widget/j;II)V
    .locals 0

    iput p3, p0, Lcom/heytap/wearable/support/widget/i;->a:I

    iput-object p1, p0, Lcom/heytap/wearable/support/widget/i;->b:Lcom/heytap/wearable/support/widget/j;

    iput p2, p0, Lcom/heytap/wearable/support/widget/i;->c:I

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 5

    const/4 v0, 0x0

    const/4 v1, 0x2

    iget v2, p0, Lcom/heytap/wearable/support/widget/i;->a:I

    iget v3, p0, Lcom/heytap/wearable/support/widget/i;->c:I

    iget-object v4, p0, Lcom/heytap/wearable/support/widget/i;->b:Lcom/heytap/wearable/support/widget/j;

    packed-switch v2, :pswitch_data_0

    goto :goto_0

    :pswitch_0
    iget-object v2, v4, Lcom/heytap/wearable/support/widget/j;->a:Landroid/os/linearmotorvibrator/LinearmotorVibrator;

    new-instance v4, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;

    invoke-direct {v4}, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;-><init>()V

    invoke-virtual {v4, v3}, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;->setEffectType(I)Landroid/os/linearmotorvibrator/WaveformEffect$Builder;

    move-result-object v3

    invoke-virtual {v3, v1}, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;->setEffectStrength(I)Landroid/os/linearmotorvibrator/WaveformEffect$Builder;

    move-result-object v1

    invoke-virtual {v1, v0}, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;->setEffectLoop(Z)Landroid/os/linearmotorvibrator/WaveformEffect$Builder;

    move-result-object v0

    invoke-virtual {v0}, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;->build()Landroid/os/linearmotorvibrator/WaveformEffect;

    move-result-object v0

    invoke-virtual {v2, v0}, Landroid/os/linearmotorvibrator/LinearmotorVibrator;->vibrate(Landroid/os/linearmotorvibrator/WaveformEffect;)V

    return-void

    :goto_0
    iget-object v2, v4, Lcom/heytap/wearable/support/widget/j;->a:Landroid/os/linearmotorvibrator/LinearmotorVibrator;

    new-instance v4, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;

    invoke-direct {v4}, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;-><init>()V

    invoke-virtual {v4, v3}, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;->setEffectType(I)Landroid/os/linearmotorvibrator/WaveformEffect$Builder;

    move-result-object v3

    invoke-virtual {v3, v1}, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;->setEffectStrength(I)Landroid/os/linearmotorvibrator/WaveformEffect$Builder;

    move-result-object v1

    invoke-virtual {v1, v0}, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;->setEffectLoop(Z)Landroid/os/linearmotorvibrator/WaveformEffect$Builder;

    move-result-object v0

    invoke-virtual {v0}, Landroid/os/linearmotorvibrator/WaveformEffect$Builder;->build()Landroid/os/linearmotorvibrator/WaveformEffect;

    move-result-object v0

    invoke-virtual {v2, v0}, Landroid/os/linearmotorvibrator/LinearmotorVibrator;->vibrate(Landroid/os/linearmotorvibrator/WaveformEffect;)V

    return-void

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
    .end packed-switch
.end method
