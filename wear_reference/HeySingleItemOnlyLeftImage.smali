.class public Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;
.super Lcom/heytap/wearable/support/widget/HeySingleBaseItem;
.source "SourceFile"


# instance fields
.field public e:Lcom/heytap/wearable/support/widget/HeyRoundImageAntiAlias;

.field public final f:Landroid/graphics/drawable/Drawable;


# direct methods
.method public constructor <init>(Landroid/content/Context;Landroid/util/AttributeSet;)V
    .locals 2

    invoke-direct {p0, p1, p2}, Lcom/heytap/wearable/support/widget/HeySingleBaseItem;-><init>(Landroid/content/Context;Landroid/util/AttributeSet;)V

    sget-object v0, Lcom/oplus/wearable/support/widget/R$styleable;->HeySingleItemWithLeftImage:[I

    const/4 v1, 0x0

    invoke-virtual {p1, p2, v0, v1, v1}, Landroid/content/Context;->obtainStyledAttributes(Landroid/util/AttributeSet;[III)Landroid/content/res/TypedArray;

    move-result-object p2

    sget v0, Lcom/oplus/wearable/support/widget/R$styleable;->HeySingleItemWithLeftImage_heyLeftImage:I

    invoke-virtual {p2, v0}, Landroid/content/res/TypedArray;->getDrawable(I)Landroid/graphics/drawable/Drawable;

    move-result-object v0

    iput-object v0, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->f:Landroid/graphics/drawable/Drawable;

    sget v0, Lcom/oplus/wearable/support/widget/R$styleable;->HeySingleItemWithLeftImage_heyText:I

    invoke-virtual {p2, v0}, Landroid/content/res/TypedArray;->getString(I)Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/heytap/wearable/support/widget/HeySingleBaseItem;->b:Ljava/lang/String;

    invoke-virtual {p2}, Landroid/content/res/TypedArray;->recycle()V

    invoke-virtual {p1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object p2

    sget v0, Lcom/oplus/wearable/support/widget/R$dimen;->hey_item_left_image_padding_vertical:I

    invoke-virtual {p2, v0}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v1

    iput v1, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->mPaddingTop:I

    invoke-virtual {p2, v0}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v0

    iput v0, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->mPaddingBottom:I

    sget v0, Lcom/oplus/wearable/support/widget/R$dimen;->hey_listitem_widget_padding_left:I

    invoke-virtual {p2, v0}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result v0

    iput v0, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->mPaddingLeft:I

    sget v0, Lcom/oplus/wearable/support/widget/R$dimen;->hey_listitem_padding_right:I

    invoke-virtual {p2, v0}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result p2

    iput p2, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->mPaddingRight:I

    invoke-virtual {p0, p1}, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->b(Landroid/content/Context;)V

    return-void
.end method


# virtual methods
.method public final a(Landroid/content/Context;)V
    .locals 1

    sget v0, Lcom/oplus/wearable/support/widget/R$layout;->hey_single_only_left_image_item_view:I

    invoke-static {p1, v0, p0}, Landroid/view/View;->inflate(Landroid/content/Context;ILandroid/view/ViewGroup;)Landroid/view/View;

    return-void
.end method

.method public final b(Landroid/content/Context;)V
    .locals 3

    invoke-super {p0, p1}, Lcom/heytap/wearable/support/widget/HeySingleBaseItem;->b(Landroid/content/Context;)V

    iget p1, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->mPaddingLeft:I

    iget v0, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->mPaddingTop:I

    iget v1, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->mPaddingRight:I

    iget v2, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->mPaddingBottom:I

    invoke-virtual {p0, p1, v0, v1, v2}, Landroid/view/View;->setPadding(IIII)V

    sget p1, Lcom/oplus/wearable/support/widget/R$id;->iv_left_icon:I

    invoke-virtual {p0, p1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p1

    check-cast p1, Lcom/heytap/wearable/support/widget/HeyRoundImageAntiAlias;

    iput-object p1, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->e:Lcom/heytap/wearable/support/widget/HeyRoundImageAntiAlias;

    iget-object v0, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->f:Landroid/graphics/drawable/Drawable;

    invoke-virtual {p1, v0}, Lcom/heytap/wearable/support/widget/HeyRoundImageAntiAlias;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    iget-object p1, p0, Lcom/heytap/wearable/support/widget/HeySingleBaseItem;->a:Landroid/widget/TextView;

    iget-object v0, p0, Lcom/heytap/wearable/support/widget/HeySingleBaseItem;->b:Ljava/lang/String;

    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    return-void
.end method

.method public getLeftImageView()Landroid/widget/ImageView;
    .locals 1

    iget-object v0, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->e:Lcom/heytap/wearable/support/widget/HeyRoundImageAntiAlias;

    return-object v0
.end method

.method public setEnabled(Z)V
    .locals 4

    invoke-super {p0, p1}, Landroid/view/View;->setEnabled(Z)V

    iget-object v0, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->e:Lcom/heytap/wearable/support/widget/HeyRoundImageAntiAlias;

    const/high16 v1, 0x3f800000    # 1.0f

    const v2, 0x3ecccccd    # 0.4f

    if-eqz v0, :cond_1

    if-eqz p1, :cond_0

    move v3, v1

    goto :goto_0

    :cond_0
    move v3, v2

    :goto_0
    invoke-virtual {v0, v3}, Landroid/view/View;->setAlpha(F)V

    :cond_1
    iget-object v0, p0, Lcom/heytap/wearable/support/widget/HeySingleBaseItem;->a:Landroid/widget/TextView;

    if-eqz v0, :cond_3

    if-eqz p1, :cond_2

    goto :goto_1

    :cond_2
    move v1, v2

    :goto_1
    invoke-virtual {v0, v1}, Landroid/view/View;->setAlpha(F)V

    :cond_3
    return-void
.end method

.method public setLeftImageView(Landroid/graphics/drawable/Drawable;)V
    .locals 1

    iget-object v0, p0, Lcom/heytap/wearable/support/widget/HeySingleItemOnlyLeftImage;->e:Lcom/heytap/wearable/support/widget/HeyRoundImageAntiAlias;

    invoke-virtual {v0, p1}, Lcom/heytap/wearable/support/widget/HeyRoundImageAntiAlias;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    return-void
.end method
