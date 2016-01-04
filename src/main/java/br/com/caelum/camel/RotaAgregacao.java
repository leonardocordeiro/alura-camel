package br.com.caelum.camel;

import java.util.Scanner;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class RotaAgregacao {
	public static void main(String[] args) throws Exception {
		CamelContext context = new DefaultCamelContext();
		context.addComponent("activemq", ActiveMQComponent.activeMQComponent("tcp://localhost:61616"));
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from("activemq:queue:pedidos"). 
					filter().
					xpath("/pedido/pagamento/status[text()='CONFIRMADO']").
					setHeader("titular", xpath("/pedido/pagamento/titular/text()")).
		        to("validator:pedido.xsd").
		        to("direct:combinaProdutoComUsuario");
		        
				from("file://usuarios?noop=true&delay=1").
					setHeader("titular", xpath("/usuario/titular/text()")).
				to("direct:combinaProdutoComUsuario");
				
				from("direct:combinaProdutoComUsuario").
					aggregate(header("titular"), new AggregationStrategy() {
						
						@Override
						public Exchange aggregate(Exchange arg0, Exchange arg1) {
							if (arg0 == null) {
								return arg1;
							}
							
							String primeira = arg0.getIn().getBody(String.class);
							String segunda = arg1.getIn().getBody(String.class);

							arg1.getIn().setBody(primeira + segunda);
							return arg1;
						}
					}).
					completionSize(2).
					log("${body}").
				to("mock:oi");
					
					
			}
		});
		
		context.start();
		new Scanner(System.in).next();
		context.stop();
	}
}
