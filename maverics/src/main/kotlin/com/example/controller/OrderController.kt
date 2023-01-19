package com.example.controller

import com.example.model.Order
import com.example.model.Transaction
import com.example.model.User
import com.example.validations.OrderValidation
import com.example.validations.UserValidation
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.json.tree.JsonObject
import java.lang.Integer.min

var orderList= mutableListOf<Order>()
var orderID=-1;

var transactions: MutableMap<Int,MutableList<Pair<Int,Int>>> =  mutableMapOf()
/// quantity--price

@Controller("/user")
class OrderController {

    @Post("/{username}/order")
    fun register(@Body body: JsonObject,@PathVariable username:String): HttpResponse<*> {
        if(UserValidation.isUserExist(username)) {
            orderID++;
            var currentOrder = Order()

            var transT : MutableList<Pair<Int,Int>> = mutableListOf()


            currentOrder.orderId = orderID;
            currentOrder.quantity = body["quantity"].intValue;
            currentOrder.type = body["type"].stringValue;
            currentOrder.price = body["price"].intValue;
            currentOrder.status = "unfilled";
            currentOrder.userName = username;


            var n = orderList.size

            if (currentOrder.type == "BUY") {
                var orderAmount = currentOrder.price * currentOrder.quantity;

                if (!OrderValidation().ifSufficientAmountInWallet(username, orderAmount)) {
                    val response = mutableMapOf<String, MutableList<String>>();
                    var errorList = mutableListOf<String>("Insufficient amount in wallet")
                    response["error"] = errorList;

                    return HttpResponse.badRequest(response);
                }

                orderList.add(currentOrder);
                transactions.put(orderID,transT)
                n = orderList.size


                walletList.get(username)!!.lockedAmount += (currentOrder.quantity * currentOrder.price)
                walletList.get(username)!!.freeAmount -= (currentOrder.quantity * currentOrder.price)



                while (true) {

                    if (currentOrder.quantity == 0)
                        break;

                    var minSellerPrice = 1000000000;
                    var orderID = -1;

                    for (orderNumber in 0..n - 2) {
                        var orderPrev = orderList[orderNumber]

                        if ((orderPrev.status != "filled") && (currentOrder.type != orderPrev.type) && (currentOrder.price>=orderPrev.price)) {
                            if (orderPrev.price < minSellerPrice) {
                                minSellerPrice = orderPrev.price
                                orderID = orderPrev.orderId
                            }
                        }
                    }

                    if (orderID != -1) {
                        var transQuantity = min(orderList[orderID].quantity, currentOrder.quantity)

                        orderList[orderID].quantity -= transQuantity
                        currentOrder.quantity -= transQuantity

                        walletList.get(username)!!.lockedAmount -= ((currentOrder.price - minSellerPrice) * transQuantity)
                        walletList.get(username)!!.freeAmount += ((currentOrder.price - minSellerPrice) * transQuantity)

                        walletList.get(username)!!.lockedAmount -= (transQuantity * minSellerPrice)
                        walletList.get(orderList.get(orderID).userName)!!.freeAmount += (transQuantity * minSellerPrice)

                        inventorMap.get(orderList.get(orderID).userName)!!.lockESOP -= (transQuantity)
                        inventorMap.get(username)!!.freeESOP += (transQuantity)


                        var tmpList: MutableList<Pair<Int, Int>> = mutableListOf()

                        if (!transactions.containsKey(currentOrder.orderId)) {
                            transactions.put(currentOrder.orderId, tmpList)
                        }
                        if (!transactions.containsKey(orderID)) {
                            transactions.put(orderID, tmpList)
                        }


                        tmpList = transactions.get(currentOrder.orderId)!!
                        tmpList.add(Pair(transQuantity, minSellerPrice))

                        tmpList = transactions.get(orderID)!!
                        tmpList.add(Pair(transQuantity, minSellerPrice))





                        currentOrder.status = "partially filled"
                        orderList[orderID].status = "partially filled"

                        if (currentOrder.quantity == 0)
                            currentOrder.status = "filled"
                        if (orderList[orderID].quantity == 0)
                            orderList[orderID].status = "filled"

                    } else
                        break;
                }

            } else {
                if (!OrderValidation().ifSufficientQuantity(username, currentOrder.quantity)) {
                    val response = mutableMapOf<String, MutableList<String>>();
                    var errorList = mutableListOf<String>("Insufficient quantity of ESOPs")
                    response["error"] = errorList;

                    return HttpResponse.badRequest(response);
                }

                orderList.add(currentOrder);
                transactions.put(orderID,transT);
                n = orderList.size

                inventorMap.get(username)!!.lockESOP += (currentOrder.quantity)
                inventorMap.get(username)!!.freeESOP -= (currentOrder.quantity)

                while (true) {

                    if (currentOrder.quantity == 0)
                        break;

                    var minSellerPrice = -1;
                    var orderID = -1;

                    for (orderNumber in 0..n - 2) {


                        var orderPrev = orderList[orderNumber]

                        if ((orderPrev.status != "filled") && (currentOrder.type != orderPrev.type) && (currentOrder.price<=orderPrev.price)) {

                            if (orderPrev.price > minSellerPrice) {
                                minSellerPrice = orderPrev.price
                                orderID = orderPrev.orderId
                            }
                        }
                    }
                    println(minSellerPrice)
                    println(orderID)

                    if (orderID != -1) {


                        var transQuantity = min(orderList[orderID].quantity, currentOrder.quantity)

                        orderList[orderID].quantity -= transQuantity
                        currentOrder.quantity -= transQuantity

                        var valOf :Int =((minSellerPrice-currentOrder.price) * transQuantity)
                        println(valOf)
                        walletList.get(orderList.get(orderID).userName)!!.lockedAmount -= valOf
                        walletList.get(orderList.get(orderID).userName)!!.freeAmount += valOf


                        walletList.get(username)!!.freeAmount += (transQuantity * minSellerPrice)
                        walletList.get(orderList.get(orderID).userName)!!.lockedAmount -= (transQuantity * minSellerPrice)

                        inventorMap.get(orderList.get(orderID).userName)!!.freeESOP += (transQuantity)
                        inventorMap.get(username)!!.lockESOP -= (transQuantity)


                        var tmpList: MutableList<Pair<Int, Int>> = mutableListOf()





                        if (!transactions.containsKey(currentOrder.orderId)) {
                            transactions.put(currentOrder.orderId, tmpList)
                        }
                        if (!transactions.containsKey(orderList.get(orderID).orderId)) {
                            transactions.put(orderList.get(orderID).orderId, tmpList)
                        }


                        tmpList = transactions.get(currentOrder.orderId)!!
                        tmpList.add(Pair(transQuantity, minSellerPrice))

                        tmpList = transactions.get(orderList.get(orderID).orderId)!!
                        tmpList.add(Pair(transQuantity, minSellerPrice))







                        currentOrder.status = "partially filled"
                        orderList[orderID].status = "partially filled"


                        if (currentOrder.quantity == 0)
                            currentOrder.status = "filled"
                        if (orderList[orderID].quantity == 0)
                            orderList[orderID].status = "filled"

                    } else
                        break;
                }


            }


            var ret:Order = Order()
            ret.orderId = currentOrder.orderId+1
            ret.userName = currentOrder.userName
            ret.quantity = currentOrder.quantity
            ret.status = currentOrder.status
            ret.price = currentOrder.price

            return HttpResponse.ok(ret);
        }else
        {
            val response = mutableMapOf<String, MutableList<String>>();
            var errorList = mutableListOf<String>("User doesn't exist.")
            response["error"] = errorList;
            return HttpResponse.badRequest(response);
        }

    }
}

