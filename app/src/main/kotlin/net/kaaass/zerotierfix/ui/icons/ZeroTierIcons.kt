package net.kaaass.zerotierfix.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

/**
 * 项目自绘图标。
 *
 * material-icons-core 未提供月球与行星图标，这里按 ZeroTier 的语义补齐：
 * Moon 表示卫星（卫星节点），Planet 表示行星。
 */
object ZeroTierIcons {
    /** 卫星节点：卫星天线。 */
    val Moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "ZeroTierIcons.Moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathParser().parsePathString(SATELLITE_DISH_PATH).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ).build()
    }

    /** 行星：带环行星。 */
    val Planet: ImageVector by lazy {
        ImageVector.Builder(
            name = "ZeroTierIcons.Planet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            // Material Symbols 的视图原点在左上角下方 960，需整体下移回 (0, 0)。
            group(translationY = 960f) {
                addPath(
                    pathData = PathParser().parsePathString(PLANET_PATH).toNodes(),
                    fill = SolidColor(Color.Black)
                )
            }
        }.build()
    }
}

/** 卫星天线：碟面 + 支架 + 两道信号弧。 */
private const val SATELLITE_DISH_PATH =
    "M4,10a7.31,7.31 0 0,0 10,10Z " +
        "M9,15L12,12 " +
        "M17,13a6,6 0 0,0 -6,-6 " +
        "M21,13A10,10 0 0,0 11,3"

/** 带环行星（取自 Material Symbols planet 图标轮廓）。 */
private const val PLANET_PATH =
    "M484.5-553.5Q499-568 499-589t-14.5-35.5Q470-639 449-639t-35.5 14.5Q399-610 399-589t14.5 " +
        "35.5Q428-539 449-539t35.5-14.5ZM822-80q-42 0-113-35t-152-95q-19 5-38.5 " +
        "7.5T479-200q-117 0-198-81t-81-198q0-20 3-40t8-39q-59-81-94.5-151.5T81-822q0-27 " +
        "15-42.5t41-15.5q26 0 67.5 18T319-801q-21 11-39 23t-35 26q-19-11-37-19t-38-17q18 38 " +
        "38.5 74t43.5 71q38-54 97-85t130-31q117 0 198.5 81.5T759-479q0 71-31.5 " +
        "130T642-252q35 23 71.5 44t74.5 38q-8-19-16.5-37T752-244q15-17 27-36t22-39q46 78 " +
        "62.5 116.5T880-138q0 29-16 43.5T822-80ZM577.5-370.5Q589-382 589-399t-11.5-28.5Q566-439 " +
        "549-439t-28.5 11.5Q509-416 509-399t11.5 28.5Q532-359 549-359t28.5-11.5Zm43-137Q629-516 " +
        "629-529t-8.5-21.5Q612-559 599-559t-21.5 8.5Q569-542 569-529t8.5 21.5Q586-499 " +
        "599-499t21.5-8.5ZM468-281q-51-44-98-91t-90-98q2 38 17 71.5t41 59.5q26 26 59 " +
        "41t71 17Zm103-21q48-25 78-72.5T679-480q0-83-58.5-141T479-679q-58 0-105 30t-72 " +
        "78q57 76 125 144t144 125Zm-197-73Zm117-116Z"
