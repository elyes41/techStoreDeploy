package tn.techstore.orders;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.techstore.orders.OrderDtos.CheckoutRequest;
import tn.techstore.orders.OrderDtos.OrderDetail;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderService orders;

    @POST
    public Response checkout(@Valid CheckoutRequest request) {
        OrderDetail created = orders.checkout(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /** Suivi de commande par le client : référence + numéro de téléphone. */
    @GET
    @Path("/{reference}")
    public OrderDetail track(@PathParam("reference") String reference, @QueryParam("phone") String phone) {
        return orders.track(reference, phone == null ? "" : phone);
    }
}
