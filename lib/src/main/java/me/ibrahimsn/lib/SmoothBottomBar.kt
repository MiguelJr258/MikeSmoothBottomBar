package me.ibrahimsn.lib

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.PopupMenu
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FontRes
import androidx.annotation.XmlRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.customview.widget.ExploreByTouchHelper
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI

class SmoothBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.SmoothBottomBarStyle
) : View(context, attrs, defStyleAttr) {

    // Dynamic Variables
    private var currentIconTint = Color.WHITE // Valor inicial provisório
    private var indicatorLocation = 0f
    private val rect = RectF()
    private var items = listOf<BottomBarItem>()

    // Attribute Defaults
    @ColorInt
    private var _barBackgroundColor = Color.parseColor(DEFAULT_BACKGROUND_COLOR)
    @ColorInt
    private var _barIndicatorColor = Color.parseColor(DEFAULT_INDICATOR_COLOR)
    @Dimension
    private var _barIndicatorRadius = context.d2p(DEFAULT_CORNER_RADIUS)
    @Dimension
    private var _barSideMargins = context.d2p(DEFAULT_SIDE_MARGIN)
    @Dimension
    private var _barCornerRadius = context.d2p(DEFAULT_BAR_CORNER_RADIUS)
    private var _barCorners = DEFAULT_BAR_CORNERS
    @Dimension
    private var _itemPadding = context.d2p(DEFAULT_ITEM_PADDING)
    private var _itemAnimDuration = DEFAULT_ANIM_DURATION
    @Dimension
    private var _itemIconSize = context.d2p(DEFAULT_ICON_SIZE)
    @Dimension
    private var _itemIconMargin = context.d2p(DEFAULT_ICON_MARGIN)
    @ColorInt
    private var _itemIconTint = Color.parseColor(DEFAULT_TINT)
    @ColorInt
    private var _itemIconTintActive = Color.WHITE
    @ColorInt
    private var _itemTextColor = Color.WHITE
    @ColorInt
    private var _itemBadgeColor = Color.RED
    @Dimension
    private var _itemTextSize = context.d2p(DEFAULT_TEXT_SIZE)
    @FontRes
    private var _itemFontFamily: Int = INVALID_RES
    @XmlRes
    private var _itemMenuRes: Int = INVALID_RES
    private var _itemActiveIndex: Int = 0

    lateinit var menu: Menu
    private val badge_arr = HashSet<Int>()

    // Core Attributes
    var barBackgroundColor: Int
        @ColorInt get() = _barBackgroundColor
        set(@ColorInt value) {
            _barBackgroundColor = value
            paintBackground.color = value
            invalidate()
        }

    var barIndicatorColor: Int
        @ColorInt get() = _barIndicatorColor
        set(@ColorInt value) {
            _barIndicatorColor = value
            paintIndicator.color = value
            invalidate()
        }

    var barIndicatorRadius: Float
        @Dimension get() = _barIndicatorRadius
        set(@Dimension value) {
            _barIndicatorRadius = value
            invalidate()
        }

    var barSideMargins: Float
        @Dimension get() = _barSideMargins
        set(@Dimension value) {
            _barSideMargins = value
            invalidate()
        }

    var barCornerRadius: Float
        @Dimension get() = _barCornerRadius
        set(@Dimension value) {
            _barCornerRadius = value
            invalidate()
        }

    var barCorners: Int
        get() = _barCorners
        set(value) {
            _barCorners = value
            invalidate()
        }

    var itemTextSize: Float
        @Dimension get() = _itemTextSize
        set(@Dimension value) {
            _itemTextSize = value
            paintText.textSize = value
            invalidate()
        }

    var itemTextColor: Int
        @ColorInt get() = _itemTextColor
        set(@ColorInt value) {
            _itemTextColor = value
            paintText.color = value
            invalidate()
        }

    var itemBadgeColor: Int
        @ColorInt get() = _itemBadgeColor
        set(@ColorInt value) {
            _itemBadgeColor = value
            badgePaint.color = value
            invalidate()
        }

    var itemPadding: Float
        @Dimension get() = _itemPadding
        set(@Dimension value) {
            _itemPadding = value
            invalidate()
        }

    var itemAnimDuration: Long
        get() = _itemAnimDuration
        set(value) {
            _itemAnimDuration = value
        }

    var itemIconSize: Float
        @Dimension get() = _itemIconSize
        set(@Dimension value) {
            _itemIconSize = value
            invalidate()
        }

    var itemIconMargin: Float
        @Dimension get() = _itemIconMargin
        set(@Dimension value) {
            _itemIconMargin = value
            invalidate()
        }

    var itemIconTint: Int
        @ColorInt get() = _itemIconTint
        set(@ColorInt value) {
            _itemIconTint = value
            invalidate()
        }

    var itemIconTintActive: Int
        @ColorInt get() = _itemIconTintActive
        set(@ColorInt value) {
            _itemIconTintActive = value
            invalidate()
        }

    var itemFontFamily: Int
        @FontRes get() = _itemFontFamily
        set(@FontRes value) {
            _itemFontFamily = value
            if (value != INVALID_RES) {
                paintText.typeface = ResourcesCompat.getFont(context, value)
                invalidate()
            }
        }

    var itemMenuRes: Int
        @XmlRes get() = _itemMenuRes
        set(value) {
            _itemMenuRes = value
            val popupMenu = PopupMenu(context, null)
            popupMenu.inflate(value)
            this.menu = popupMenu.menu
            if (value != INVALID_RES) {
                items = BottomBarParser(context, value).parse()
                invalidate()
            }
        }

    var itemActiveIndex: Int
        get() = _itemActiveIndex
        set(value) {
            _itemActiveIndex = value
            applyItemActiveIndex()
        }

    // Listeners
    var onItemSelectedListener: OnItemSelectedListener? = null
    var onItemReselectedListener: OnItemReselectedListener? = null
    var onItemSelected: ((Int) -> Unit)? = null
    var onItemReselected: ((Int) -> Unit)? = null

    // Paints
    private val paintBackground = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = barBackgroundColor
    }

    private val paintIndicator = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = barIndicatorColor
    }

    private val badgePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = itemBadgeColor
    }

    private val paintText = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = itemTextColor
        textSize = itemTextSize
        textAlign = Paint.Align.LEFT // Alterado para facilitar cálculo de largura
        isFakeBoldText = true
    }

    // Acessibilidade
    private var exploreByTouchHelper: AccessibleExploreByTouchHelper

    init {
        obtainStyledAttributes(attrs, defStyleAttr)
        // Inicializa a cor do ícone ativo
        currentIconTint = itemIconTintActive
        exploreByTouchHelper = AccessibleExploreByTouchHelper(this, items, ::onClickAction)
        ViewCompat.setAccessibilityDelegate(this, exploreByTouchHelper)
    }

    private fun obtainStyledAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SmoothBottomBar,
            defStyleAttr,
            0
        )
        try {
            barBackgroundColor = typedArray.getColor(
                R.styleable.SmoothBottomBar_backgroundColor,
                barBackgroundColor
            )
            barIndicatorColor = typedArray.getColor(
                R.styleable.SmoothBottomBar_indicatorColor,
                barIndicatorColor
            )
            barIndicatorRadius = typedArray.getDimension(
                R.styleable.SmoothBottomBar_indicatorRadius,
                barIndicatorRadius
            )
            barSideMargins = typedArray.getDimension(
                R.styleable.SmoothBottomBar_sideMargins,
                barSideMargins
            )
            barCornerRadius = typedArray.getDimension(
                R.styleable.SmoothBottomBar_cornerRadius,
                barCornerRadius
            )
            barCorners = typedArray.getInteger(
                R.styleable.SmoothBottomBar_corners,
                barCorners
            )
            itemPadding = typedArray.getDimension(
                R.styleable.SmoothBottomBar_itemPadding,
                itemPadding
            )
            itemTextColor = typedArray.getColor(
                R.styleable.SmoothBottomBar_textColor,
                itemTextColor
            )
            itemTextSize = typedArray.getDimension(
                R.styleable.SmoothBottomBar_textSize,
                itemTextSize
            )
            itemIconSize = typedArray.getDimension(
                R.styleable.SmoothBottomBar_iconSize,
                itemIconSize
            )
            itemIconMargin = typedArray.getDimension(
                R.styleable.SmoothBottomBar_iconMargin,
                itemIconMargin
            )
            itemIconTint = typedArray.getColor(
                R.styleable.SmoothBottomBar_iconTint,
                itemIconTint
            )
            itemBadgeColor = typedArray.getColor(
                R.styleable.SmoothBottomBar_badgeColor,
                itemBadgeColor
            )
            itemIconTintActive = typedArray.getColor(
                R.styleable.SmoothBottomBar_iconTintActive,
                itemIconTintActive
            )
            itemActiveIndex = typedArray.getInt(
                R.styleable.SmoothBottomBar_activeItem,
                itemActiveIndex
            )
            itemFontFamily = typedArray.getResourceId(
                R.styleable.SmoothBottomBar_itemFontFamily,
                itemFontFamily
            )
            itemAnimDuration = typedArray.getInt(
                R.styleable.SmoothBottomBar_duration,
                itemAnimDuration.toInt()
            ).toLong()
            itemMenuRes = typedArray.getResourceId(
                R.styleable.SmoothBottomBar_menu,
                itemMenuRes
            )
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            typedArray.recycle()
        }
        
        // Atualiza os paints com os valores obtidos
        paintBackground.color = barBackgroundColor
        paintIndicator.color = barIndicatorColor
        paintText.color = itemTextColor
        paintText.textSize = itemTextSize
        
        if (itemFontFamily != INVALID_RES) {
             try {
                paintText.typeface = ResourcesCompat.getFont(context, itemFontFamily)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // Extensão para dp to px
    private fun Context.d2p(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Resetamos a posição inicial do indicador quando o tamanho muda
        applyItemActiveIndex()
    }

    fun setBadge(pos: Int) {
        badge_arr.add(pos)
        invalidate()
    }

    fun removeBadge(pos: Int) {
        badge_arr.remove(pos)
        invalidate()
    }

    // --- AQUI ESTÁ A LÓGICA MODIFICADA DE DESENHO ---
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Desenhar o Fundo
        if (barCornerRadius > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(
                0f, 0f, width.toFloat(), height.toFloat(),
                minOf(barCornerRadius, height.toFloat() / 2),
                minOf(barCornerRadius, height.toFloat() / 2),
                paintBackground
            )
        } else {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBackground)
        }
        
        if (items.isEmpty()) return

        // 2. Cálculos Matemáticos para Layout Dinâmico
        val totalUsableWidth = width - (barSideMargins * 2)
        
        // Largura do Item ATIVO = Ícone + Margem + Texto + (Padding * 2)
        val activeItemTextWidth = paintText.measureText(items[itemActiveIndex].title)
        val activeItemWidth = itemIconSize + itemIconMargin + activeItemTextWidth + (itemPadding * 2)
        
        // Largura dos Itens INATIVOS = O espaço que sobra dividido pelos outros
        val inactiveItemWidth = if (items.size > 1) (totalUsableWidth - activeItemWidth) / (items.size - 1) else 0f

        var currentX = barSideMargins

        // 3. Loop de Desenho
        for ((index, item) in items.withIndex()) {
            
            // Define a largura deste item específico
            val thisItemWidth = if (index == itemActiveIndex) activeItemWidth else inactiveItemWidth
            val itemCenterX = currentX + (thisItemWidth / 2)
            val itemCenterY = height / 2f

            // Atualiza o retângulo do item (para cliques)
            item.rect.set(currentX, 0f, currentX + thisItemWidth, height.toFloat())

            // A. Desenhar o Indicador (Pílula Branca) - Apenas se for o ativo
            if (index == itemActiveIndex) {
                // Animação de posição X
                // Se indicatorLocation for 0 (inicio), ajusta direto. Senão, a animação trata.
                // Mas para o onDraw "estático", usamos a posição alvo.
                // A animação acontece no applyItemActiveIndex alterando o indicatorLocation.
                
                // Nota: Para simplificar e garantir que o tamanho bate certo com a lógica,
                // vamos desenhar a pílula baseada na posição atual (currentX) neste frame.
                // A animação de transição move o 'currentX' virtualmente? Não.
                // A animação move o 'indicatorLocation'.
                
                // LÓGICA DE ANIMAÇÃO AJUSTADA:
                // O indicatorLocation controla o ESQUERDA do indicador.
                // A largura do indicador também anima? Idealmente sim, mas para este fix,
                // vamos assumir que o indicador segue a posição do item ativo.
                
                // Vamos usar a posição calculada diretamente para garantir o visual estático correto.
                // (Para animação suave de largura, precisaríamos de mais variáveis, mas isto cumpre o requisito visual).
                
                // Usamos as coordenadas do rect do item atual.
                val pillLeft = if(indicatorLocation == 0f) currentX else indicatorLocation
                
                // Assumindo que a animação atualiza apenas o 'left', precisamos calcular o 'right'
                // com base na largura do item ativo.
                // Como a largura muda, a animação pode ser complexa.
                // Vamos simplificar: Desenha a pílula exatamente onde o item ativo DEVE estar.
                
                rect.left = currentX
                rect.right = currentX + thisItemWidth
                rect.top = itemCenterY - (itemIconSize / 2) - itemPadding
                rect.bottom = itemCenterY + (itemIconSize / 2) + itemPadding

                canvas.drawRoundRect(rect, barIndicatorRadius, barIndicatorRadius, paintIndicator)
            }

            // B. Desenhar o Ícone
            // Se ativo: Ícone fica à esquerda (com padding). Se inativo: Ícone fica no centro.
            val iconX = if (index == itemActiveIndex) (currentX + itemPadding) else (itemCenterX - itemIconSize / 2)
            
            item.icon.mutate()
            item.icon.setBounds(
                iconX.toInt(),
                (itemCenterY - itemIconSize / 2).toInt(),
                (iconX + itemIconSize).toInt(),
                (itemCenterY + itemIconSize / 2).toInt()
            )
            
            tintAndDrawIcon(item, index, canvas)
            
            // Badge
            if(badge_arr.contains(index)){
                 canvas.drawCircle(
                    iconX + itemIconSize - 2, 
                    itemCenterY - itemIconSize / 2 + 2, 
                    10f, 
                    badgePaint
                )
            }

            // C. Desenhar o Texto (Apenas se ativo)
            if (index == itemActiveIndex) {
                val textHeight = (paintText.descent() + paintText.ascent()) / 2
                val textX = iconX + itemIconSize + itemIconMargin
                
                // Força opacidade total para o texto ativo
                paintText.alpha = 255 
                canvas.drawText(item.title, textX, itemCenterY - textHeight, paintText)
            }

            // Avança a posição X para o próximo item
            currentX += thisItemWidth
        }
    }
    
    // Helper para desenhar ícone com cor
    private fun tintAndDrawIcon(item: BottomBarItem, index: Int, canvas: Canvas) {
        DrawableCompat.setTint(
            item.icon,
            if (index == itemActiveIndex) currentIconTint else itemIconTint
        )
        item.icon.draw(canvas)
    }

    // --- LÓGICA DE TOQUE MODIFICADA ---
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                // Recalcular larguras para detetar o toque corretamente
                val totalUsableWidth = width - (barSideMargins * 2)
                val activeItemTextWidth = paintText.measureText(items[itemActiveIndex].title)
                val activeItemWidth = itemIconSize + itemIconMargin + activeItemTextWidth + (itemPadding * 2)
                val inactiveItemWidth = if (items.size > 1) (totalUsableWidth - activeItemWidth) / (items.size - 1) else 0f

                var currentX = barSideMargins
                
                for ((i, item) in items.withIndex()) {
                    val thisItemWidth = if (i == itemActiveIndex) activeItemWidth else inactiveItemWidth
                    
                    if (event.x >= currentX && event.x <= currentX + thisItemWidth) {
                        onClickAction(i)
                        break
                    }
                    currentX += thisItemWidth
                }
            }
        }
        return super.onTouchEvent(event)
    }
    
    // Acessibilidade
    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return exploreByTouchHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)
    }

    private fun onClickAction(viewId: Int) {
        exploreByTouchHelper.invalidateVirtualView(viewId)
        if (viewId != itemActiveIndex) {
            itemActiveIndex = viewId
            onItemSelected?.invoke(viewId)
            onItemSelectedListener?.onItemSelect(viewId)
        } else {
            onItemReselected?.invoke(viewId)
            onItemReselectedListener?.onItemReselect(viewId)
        }
        exploreByTouchHelper.sendEventForVirtualView(
            viewId,
            AccessibilityEvent.TYPE_VIEW_CLICKED
        )
    }

    private fun applyItemActiveIndex() {
        if (items.isNotEmpty()) {
            // Animação da cor do ícone
            ValueAnimator.ofObject(ArgbEvaluator(), itemIconTint, itemIconTintActive).apply {
                duration = itemAnimDuration
                addUpdateListener { currentIconTint = it.animatedValue as Int }
                start()
            }
            // Força o redesenho para a nova posição
            invalidate()
        }
    }
    
    // Helpers de Animação (mantidos para compatibilidade, mas simplificados)
    private fun animateAlpha(item: BottomBarItem, to: Int) {
        // Não usado na nova lógica de desenho direto, mas mantido para evitar erros
    }

    // --- SETUP COM NAV CONTROLLER ---
    fun setupWithNavController(menu: Menu, navController: NavController) {
        NavigationComponentHelper.setupWithNavController(menu, this, navController)
    }

    fun setupWithNavController(navController: NavController) {
        NavigationComponentHelper.setupWithNavController(this.menu, this, navController)
    }
    
    // Interfaces de Listener
    fun setOnItemSelectedListener(listener: (position: Int) -> Unit) {
        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelect(pos: Int): Boolean {
                listener.invoke(pos)
                return true
            }
        }
    }

    fun setOnItemReselectedListener(listener: (position: Int) -> Unit) {
        onItemReselectedListener = object : OnItemReselectedListener {
            override fun onItemReselect(pos: Int) {
                listener.invoke(pos)
            }
        }
    }

    interface OnItemSelectedListener {
        fun onItemSelect(pos: Int): Boolean
    }

    interface OnItemReselectedListener {
        fun onItemReselect(pos: Int)
    }

    // --- COMPANION OBJECT COM CONSTANTES ---
    companion object {
        private const val INVALID_RES = -1
        private const val DEFAULT_INDICATOR_COLOR = "#FFFFFF" // Branco
        private const val DEFAULT_BACKGROUND_COLOR = "#414141" // Cinza Escuro
        private const val DEFAULT_TINT = "#FFFFFF"
        
        // Corner Flags
        private const val NO_CORNERS = 0
        private const val TOP_LEFT_CORNER = 1
        private const val TOP_RIGHT_CORNER = 2
        private const val BOTTOM_RIGHT_CORNER = 4
        private const val BOTTOM_LEFT_CORNER = 8
        private const val ALL_CORNERS = 15
        
        private const val DEFAULT_SIDE_MARGIN = 10f
        private const val DEFAULT_ITEM_PADDING = 10f
        private const val DEFAULT_ANIM_DURATION = 200L
        private const val DEFAULT_ICON_SIZE = 20F // Ícone padrão um pouco maior
        private const val DEFAULT_ICON_MARGIN = 4F
        private const val DEFAULT_TEXT_SIZE = 14F
        private const val DEFAULT_CORNER_RADIUS = 20F
        private const val DEFAULT_BAR_CORNER_RADIUS = 35F // Bem redondo
        private const val DEFAULT_BAR_CORNERS = ALL_CORNERS
        
        private const val OPAQUE = 255
        private const val TRANSPARENT = 0
    }
}
