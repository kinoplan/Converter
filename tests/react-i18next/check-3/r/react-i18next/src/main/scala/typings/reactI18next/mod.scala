package typings.reactI18next

import typings.reactI18next.reactI18nextBooleans.`false`
import org.scalablytyped.runtime.StObject
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSGlobal, JSImport, JSName, JSBracketAccess}

object mod {
  
  @JSImport("react-i18next", "Trans")
  @js.native
  val Trans: TransLegacy = js.native
  
  @JSImport("react-i18next", "useTranslation")
  @js.native
  val useTranslation: UseTranslationSelector = js.native
  
  type EnableSelector = `false`
  
  trait IcuTransComponent extends StObject
  
  trait IcuTransWithoutContextComponent extends StObject
  
  trait TransLegacy extends StObject
  
  trait TransSelector extends StObject
  
  type UseTranslationLegacy = js.Function1[/* ns */ js.UndefOr[String], Unit]
  
  @js.native
  trait UseTranslationSelector extends StObject {
    
    def apply(): Unit = js.native
    def apply(ns: String): Unit = js.native
  }
  
  trait Wrapper[T] extends StObject {
    
    var ctx: T
  }
  object Wrapper {
    
    inline def apply[T](ctx: T): Wrapper[T] = {
      val __obj = js.Dynamic.literal(ctx = ctx.asInstanceOf[js.Any])
      __obj.asInstanceOf[Wrapper[T]]
    }
    
    @scala.inline
    implicit open class MutableBuilder[Self <: Wrapper[?], T] (val x: Self & Wrapper[T]) extends AnyVal {
      
      inline def setCtx(value: T): Self = StObject.set(x, "ctx", value.asInstanceOf[js.Any])
    }
  }
}
