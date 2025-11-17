package Pagos

import retrofit2.Call
import retrofit2.http.*

interface PagosApiService {

    @GET("cuotas")
    fun getCuotas(): Call<CuotaListResponse>

    @POST("cuotas")
    fun createCuota(@Body cuota: Cuota): Call<GenericCuotaResponse>

    @GET("pagos")
    fun getPagos(): Call<PagoListResponse>

    @POST("pagos")
    fun createPago(@Body pago: Pago): Call<GenericPagoResponse>
}
