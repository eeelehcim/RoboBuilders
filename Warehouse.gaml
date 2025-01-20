/**
* Name: test
* Based on the internal empty template. 
* Author: julia
* Tags: 
*/

model test
global {   
    graph movement_graph <- spatial_graph([]);
    list<point> drop_points;
    list<point> pickup_points;
    list<point> charging_stations;
    list<point> triage_stations;
    list<point> obstacle_points;
    
    int rotation <- 120;
 
    init {
		    	
        // Add drop points
		add point(10, 90) to: drop_points;   // Top-left drop point (red circle)
		add point(90, 90) to: drop_points;   // Top-right drop point (blue circle)
		add point(10, 10) to: drop_points;   // Bottom-left drop point
		
		// Add charging stations
		add point(10, 50) to: charging_stations;   // Midpoint between red and blue drop points
		add point(90, 10) to: charging_stations;   // Bottom-right charging station
		
		// Add triage station
		add point(90, 50) to: triage_stations;   // Central triage station
		
		// Add obstacles
		add point(60, 80) to: obstacle_points;   // Top-central obstacle
		add point(30, 20) to: obstacle_points;   // Bottom-central obstacle
		
		// Add crate points
		add point(30, 70) to: pickup_points;   // Near the top-left
		add point(70, 30) to: pickup_points;   // Near the bottom-right


        // Add all points to the graph
        loop node over: drop_points {
            movement_graph <- movement_graph add_node(node);
        }
        loop node over: charging_stations {
            movement_graph <- movement_graph add_node(node);
        }
        loop node over: triage_stations {
            movement_graph <- movement_graph add_node(node);
        }
        loop node over: pickup_points {
            movement_graph <- movement_graph add_node(node);
        }
 
        // Set edge weights to their perimeter for consistent movement speed
        movement_graph <- movement_graph with_weights(
            movement_graph.edges as_map(each::geometry(each).perimeter)
        );
 
        // Create robots
        create robot number: 1;
    }
}
 
species robot {
	float size <- 1.0;
	rgb color <- #black;
	int _rotation;
	geometry rect;
	int crates_visited <- 0;
	float speed <- 1.0; // Movement speed
	
//	float rotation <- 120.0;  // storingt he angle of the sprite representing the robot.
	init {
		location <- point(50,50);
		_rotation <- rotation;
		//do add_desire(has_crate);
	}
	aspect base {
	    // Calculate the direction vector for the front of the robot
	    point r <- point(sin(_rotation), -cos(_rotation));
	
	    // Robot body (rectangle)
	    rect <- (rectangle(1, 20) translated_by point(r * 10)) rotated_by _rotation;
	    draw rect color: #yellow;
	
	    // Robot indicator (front triangle)
	    draw triangle(6) rotated_by _rotation color: color;
	
	    // side lines
	    draw line([location + point(-10 * cos(_rotation + 180), -10 * sin(_rotation + 180)),
	               location + point(10 * cos(_rotation + 180), 10 * sin(_rotation + 180))]) color: #green;
	}
}

 

experiment WarehouseExperiment type: gui {
    output {
        display WarehouseDisplay type: 2d {
            graphics "Warehouse Layout" {
                // Draw drop points
                loop node over: drop_points {
                    draw circle(3) at: node color: #yellow;
                    draw "Drop" at: node color: #black;
                }
 
                // Draw charging stations
                loop node over: charging_stations {
                    draw circle(3) at: node color: #green;
                    draw "Charging" at: node color: #black;
                }
 
                // Draw triage station
                loop node over: triage_stations {
                    draw circle(3) at: node color: #red;
                    draw "Triage" at: node color: #black;
                }
 
                // Draw obstacles
                loop node over: obstacle_points {
                    draw square(5) at: node color: #gray;
                    draw "Obstacle" at: node color: #black;
                }
 
                // Draw crates
                loop node over: pickup_points {
                    draw circle(2) at: node color: #brown;
                    draw "Pickup" at: node color: #black;
                }
 
                // Draw edges of the graph
                loop edge over: movement_graph.edges {
                    draw geometry(edge) color: #black;
                }
            }
 
            // Draw robot visuals
            species robot aspect: base;

        }
    }
}