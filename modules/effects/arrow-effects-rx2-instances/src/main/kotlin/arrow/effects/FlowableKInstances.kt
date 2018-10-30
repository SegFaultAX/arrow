package arrow.effects

import arrow.Kind
import arrow.core.Either
import arrow.core.Eval
import arrow.deprecation.ExtensionsDSLDeprecated
import arrow.effects.typeclasses.Async
import arrow.effects.typeclasses.Bracket
import arrow.effects.typeclasses.ConcurrentEffect
import arrow.effects.typeclasses.Disposable
import arrow.effects.typeclasses.Effect
import arrow.effects.typeclasses.ExitCase
import arrow.effects.typeclasses.MonadDefer
import arrow.effects.typeclasses.Proc
import arrow.extension
import arrow.typeclasses.Applicative
import arrow.typeclasses.ApplicativeError
import arrow.typeclasses.Foldable
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadError
import arrow.typeclasses.Traverse
import io.reactivex.BackpressureStrategy
import kotlin.coroutines.CoroutineContext

@extension
interface FlowableKFunctorInstance : Functor<ForFlowableK> {
  override fun <A, B> Kind<ForFlowableK, A>.map(f: (A) -> B): FlowableK<B> =
    fix().map(f)
}

@extension
interface FlowableKApplicativeInstance : Applicative<ForFlowableK> {
  override fun <A, B> FlowableKOf<A>.ap(ff: FlowableKOf<(A) -> B>): FlowableK<B> =
    fix().ap(ff)

  override fun <A, B> Kind<ForFlowableK, A>.map(f: (A) -> B): FlowableK<B> =
    fix().map(f)

  override fun <A> just(a: A): FlowableK<A> =
    FlowableK.just(a)
}

@extension
interface FlowableKMonadInstance : Monad<ForFlowableK> {
  override fun <A, B> FlowableKOf<A>.ap(ff: FlowableKOf<(A) -> B>): FlowableK<B> =
    fix().ap(ff)

  override fun <A, B> FlowableKOf<A>.flatMap(f: (A) -> Kind<ForFlowableK, B>): FlowableK<B> =
    fix().flatMap(f)

  override fun <A, B> FlowableKOf<A>.map(f: (A) -> B): FlowableK<B> =
    fix().map(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, FlowableKOf<arrow.core.Either<A, B>>>): FlowableK<B> =
    FlowableK.tailRecM(a, f)

  override fun <A> just(a: A): FlowableK<A> =
    FlowableK.just(a)
}

@extension
interface FlowableKFoldableInstance : Foldable<ForFlowableK> {
  override fun <A, B> Kind<ForFlowableK, A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> Kind<ForFlowableK, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): arrow.core.Eval<B> =
    fix().foldRight(lb, f)
}

@extension
interface FlowableKTraverseInstance : Traverse<ForFlowableK> {
  override fun <A, B> Kind<ForFlowableK, A>.map(f: (A) -> B): FlowableK<B> =
    fix().map(f)

  override fun <G, A, B> FlowableKOf<A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, FlowableK<B>> =
    fix().traverse(AP, f)

  override fun <A, B> Kind<ForFlowableK, A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> Kind<ForFlowableK, A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): arrow.core.Eval<B> =
    fix().foldRight(lb, f)
}

@extension
interface FlowableKApplicativeErrorInstance :
  ApplicativeError<ForFlowableK, Throwable>,
  FlowableKApplicativeInstance {
  override fun <A> raiseError(e: Throwable): FlowableK<A> =
    FlowableK.raiseError(e)

  override fun <A> FlowableKOf<A>.handleErrorWith(f: (Throwable) -> FlowableKOf<A>): FlowableK<A> =
    fix().handleErrorWith { f(it).fix() }
}

@extension
interface FlowableKMonadErrorInstance :
  MonadError<ForFlowableK, Throwable>,
  FlowableKMonadInstance {
  override fun <A> raiseError(e: Throwable): FlowableK<A> =
    FlowableK.raiseError(e)

  override fun <A> FlowableKOf<A>.handleErrorWith(f: (Throwable) -> FlowableKOf<A>): FlowableK<A> =
    fix().handleErrorWith { f(it).fix() }
}

@extension
interface FlowableKBracketInstance : FlowableKMonadErrorInstance, Bracket<ForFlowableK, Throwable> {
  override fun <A, B> Kind<ForFlowableK, A>.bracketCase(use: (A) -> Kind<ForFlowableK, B>, release: (A, ExitCase<Throwable>) -> Kind<ForFlowableK, Unit>): FlowableK<B> =
    fix().bracketCase({ a -> use(a).fix() }, { a, e -> release(a, e).fix() })
}

@extension
interface FlowableKMonadDeferInstance :
  MonadDefer<ForFlowableK>,
  FlowableKBracketInstance {
  override fun <A> defer(fa: () -> FlowableKOf<A>): FlowableK<A> =
    FlowableK.defer(fa)

  fun BS(): BackpressureStrategy = BackpressureStrategy.BUFFER
}

@extension
interface FlowableKAsyncInstance :
  Async<ForFlowableK>,
  FlowableKMonadDeferInstance {
  override fun <A> async(fa: Proc<A>): FlowableK<A> =
    FlowableK.async(fa, BS())

  override fun <A> FlowableKOf<A>.continueOn(ctx: CoroutineContext): FlowableK<A> =
    fix().continueOn(ctx)
}

@extension
interface FlowableKEffectInstance :
  Effect<ForFlowableK>,
  FlowableKAsyncInstance {
  override fun <A> FlowableKOf<A>.runAsync(cb: (Either<Throwable, A>) -> FlowableKOf<Unit>): FlowableK<Unit> =
    fix().runAsync(cb)
}

@extension
interface FlowableKConcurrentEffectInstance : ConcurrentEffect<ForFlowableK>, FlowableKEffectInstance {
  override fun <A> Kind<ForFlowableK, A>.runAsyncCancellable(cb: (Either<Throwable, A>) -> FlowableKOf<Unit>): FlowableK<Disposable> =
    fix().runAsyncCancellable(cb)
}

fun FlowableK.Companion.monadFlat(): FlowableKMonadInstance = monad()

fun FlowableK.Companion.monadConcat(): FlowableKMonadInstance = object : FlowableKMonadInstance {
  override fun <A, B> Kind<ForFlowableK, A>.flatMap(f: (A) -> Kind<ForFlowableK, B>): FlowableK<B> =
    fix().concatMap { f(it).fix() }
}

fun FlowableK.Companion.monadSwitch(): FlowableKMonadInstance = object : FlowableKMonadInstance {
  override fun <A, B> Kind<ForFlowableK, A>.flatMap(f: (A) -> Kind<ForFlowableK, B>): FlowableK<B> =
    fix().switchMap { f(it).fix() }
}

fun FlowableK.Companion.monadErrorFlat(): FlowableKMonadErrorInstance = monadError()

fun FlowableK.Companion.monadErrorConcat(): FlowableKMonadErrorInstance = object : FlowableKMonadErrorInstance {
  override fun <A, B> Kind<ForFlowableK, A>.flatMap(f: (A) -> Kind<ForFlowableK, B>): FlowableK<B> =
    fix().concatMap { f(it).fix() }
}

fun FlowableK.Companion.monadErrorSwitch(): FlowableKMonadErrorInstance = object : FlowableKMonadErrorInstance {
  override fun <A, B> Kind<ForFlowableK, A>.flatMap(f: (A) -> Kind<ForFlowableK, B>): FlowableK<B> =
    fix().switchMap { f(it).fix() }
}

fun FlowableK.Companion.monadSuspendBuffer(): FlowableKMonadDeferInstance = monadDefer()

fun FlowableK.Companion.monadSuspendDrop(): FlowableKMonadDeferInstance = object : FlowableKMonadDeferInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.DROP
}

fun FlowableK.Companion.monadSuspendError(): FlowableKMonadDeferInstance = object : FlowableKMonadDeferInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.ERROR
}

fun FlowableK.Companion.monadSuspendLatest(): FlowableKMonadDeferInstance = object : FlowableKMonadDeferInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.LATEST
}

fun FlowableK.Companion.monadSuspendMissing(): FlowableKMonadDeferInstance = object : FlowableKMonadDeferInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.MISSING
}

fun FlowableK.Companion.asyncBuffer(): FlowableKAsyncInstance = async()

fun FlowableK.Companion.asyncDrop(): FlowableKAsyncInstance = object : FlowableKAsyncInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.DROP
}

fun FlowableK.Companion.asyncError(): FlowableKAsyncInstance = object : FlowableKAsyncInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.ERROR
}

fun FlowableK.Companion.asyncLatest(): FlowableKAsyncInstance = object : FlowableKAsyncInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.LATEST
}

fun FlowableK.Companion.asyncMissing(): FlowableKAsyncInstance = object : FlowableKAsyncInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.MISSING
}

fun FlowableK.Companion.effectBuffer(): FlowableKEffectInstance = effect()

fun FlowableK.Companion.effectDrop(): FlowableKEffectInstance = object : FlowableKEffectInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.DROP
}

fun FlowableK.Companion.effectError(): FlowableKEffectInstance = object : FlowableKEffectInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.ERROR
}

fun FlowableK.Companion.effectLatest(): FlowableKEffectInstance = object : FlowableKEffectInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.LATEST
}

fun FlowableK.Companion.effectMissing(): FlowableKEffectInstance = object : FlowableKEffectInstance {
  override fun BS(): BackpressureStrategy = BackpressureStrategy.MISSING
}

object FlowableKContext : FlowableKConcurrentEffectInstance, FlowableKTraverseInstance {
  override fun <A, B> FlowableKOf<A>.map(f: (A) -> B): FlowableK<B> =
    fix().map(f)
}

@Deprecated(ExtensionsDSLDeprecated)
infix fun <A> ForFlowableK.Companion.extensions(f: FlowableKContext.() -> A): A =
  f(FlowableKContext)
