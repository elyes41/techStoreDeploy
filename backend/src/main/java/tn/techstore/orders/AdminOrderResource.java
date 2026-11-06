package tn.techstore.orders;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tn.techstore.common.ApiException;
import tn.techstore.common.Ids;
import tn.techstore.common.PageResult;
import tn.techstore.mail.ShippingNotifier;
import tn.techstore.orders.OrderDtos.*;

@Path("/api/admin/orders")
@RolesAllowed("admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminOrderResource {

    @Inject
    OrderService orders;

    @Inject
    ShippingNotifier notifier;

    @GET
    public PageResult<OrderSummary> list(
            @QueryParam("status") OrderStatus status,
            @QueryParam("q") String q,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return orders.list(status, q, page, size);
    }

    @GET
    @Path("/{id}")
    public OrderDetail get(@PathParam("id") String id) {
        return orders.get(id);
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public OrderDetail update(@PathParam("id") String id, @Valid StatusUpdate update) {
        return orders.updateStatus(id, update);
    }

    /** Renvoie (ou envoie) l'e-mail « votre colis est parti » au client. */
    @POST
    @Path("/{id}/notify-shipped")
    public NotifyResult notifyShipped(@PathParam("id") String id) {
        OrderDetail current = orders.get(id);
        if (current.status() != OrderStatus.SHIPPED && current.status() != OrderStatus.DELIVERED) {
            throw ApiException.badRequest("La commande n'est pas encore expédiée");
        }
        String error = notifier.send(Ids.parse(id, "Commande"));
        return new NotifyResult(error == null, error == null ? "E-mail envoyé à " + current.email() : error, orders.get(id));
    }
}
