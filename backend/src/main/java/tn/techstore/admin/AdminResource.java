package tn.techstore.admin;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.techstore.admin.AdminDtos.DashboardStats;
import tn.techstore.common.PageResult;
import tn.techstore.coupons.CouponDtos.CouponDto;
import tn.techstore.coupons.CouponDtos.CouponForm;
import tn.techstore.coupons.CouponService;
import tn.techstore.reviews.Review;
import tn.techstore.reviews.ReviewDtos.AdminReviewDto;
import tn.techstore.reviews.ReviewDtos.ReviewStatusUpdate;
import tn.techstore.reviews.ReviewService;

import java.util.List;

/** Tableau de bord, modération des avis et codes promo. */
@Path("/api/admin")
@RolesAllowed("admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    StatsService stats;

    @Inject
    ReviewService reviews;

    @Inject
    CouponService coupons;

    @GET
    @Path("/stats")
    public DashboardStats stats() {
        return stats.dashboard();
    }

    // ---------- Avis ----------

    @GET
    @Path("/reviews")
    public PageResult<AdminReviewDto> reviews(@QueryParam("status") Review.Status status,
                                              @QueryParam("page") @DefaultValue("0") int page,
                                              @QueryParam("size") @DefaultValue("20") int size) {
        return reviews.list(status, page, size);
    }

    @PATCH
    @Path("/reviews/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public AdminReviewDto moderate(@PathParam("id") String id, @Valid ReviewStatusUpdate body) {
        return reviews.moderate(id, body.status());
    }

    @DELETE
    @Path("/reviews/{id}")
    public Response deleteReview(@PathParam("id") String id) {
        reviews.delete(id);
        return Response.noContent().build();
    }

    // ---------- Codes promo ----------

    @GET
    @Path("/coupons")
    public List<CouponDto> coupons() {
        return coupons.list();
    }

    @POST
    @Path("/coupons")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createCoupon(@Valid CouponForm form) {
        return Response.status(Response.Status.CREATED).entity(coupons.create(form)).build();
    }

    @PUT
    @Path("/coupons/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CouponDto updateCoupon(@PathParam("id") String id, @Valid CouponForm form) {
        return coupons.update(id, form);
    }

    @DELETE
    @Path("/coupons/{id}")
    public Response deleteCoupon(@PathParam("id") String id) {
        coupons.delete(id);
        return Response.noContent().build();
    }
}
