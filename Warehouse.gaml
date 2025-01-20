model Warehouse

/* Insert your model definition here */
global{
	string crate_at_location <- "crate_at_location";
	predicate crate_location <- new_predicate(crate_at_location);
	predicate has_crate <- new_predicate("has crate");
	predicate dropped_off_crate <- new_predicate("dropped off crate");
	predicate select_crate <- new_predicate("select crate");
	predicate rotate_towards <- new_predicate("rotating towards");
	predicate battery_low <- new_predicate("bettery low");
	predicate go_to_charge <- new_predicate("go to charge");
    float low_battery_threshold <- 20.0;
	list<crate> assigned_crates;
	int instance_count <- 1;
	int obstacle_count <- 1;
	int crate_count <- 20;


	int rotation <- 120;
	geometry obstaclesgeom;

	init {
		create robot number: instance_count;
		create crate number: crate_count;
		create obstacle number: obstacle_count;
		create drop_off_point;
		create triaging_point;
		create charging_point;
		obstaclesgeom <- union(obstacle collect each.geo);
		write obstaclesgeom;
	}
}

species robot skills: [moving] control: simple_bdi {
    point target;
    crate target_crate;
    float speed <- 2.0;
    int crates_visited <- 0;
    int direction <- 0;
    float size <- 1.0;
    rgb color <- #blue;
    geometry rect;
    geometry avoidrect;
    geometry siderect;
    float battery_level <- 100.0; // Percentuale della batteria iniziale

    init {
        float offset_x <- rnd(21) - 10;
        float offset_y <- rnd(21) - 10;
        location <- point(10 + offset_x, 10 + offset_y);
        heading <- 90;
        do add_desire(has_crate);
    }

    aspect base {
        point r <- point(sin(heading+90), -cos(heading+90));
        int rectlen <- 40;
        int avoidrectlen <- 12;
        avoidrect <- rectangle(2,avoidrectlen) translated_by point(r*avoidrectlen/2) rotated_by (heading+90);
        draw avoidrect  color:#red;
        siderect <- rectangle(6,avoidrectlen*2.3) translated_by point(-r*avoidrectlen/6) rotated_by (heading);
        draw siderect color:#red;
        draw triangle(6) rotated_by (heading+90) color: color;
    }

    rule belief: crate_location new_desire: has_crate strength: 2.0;
    rule belief: has_crate new_desire: dropped_off_crate strength: 3.0;
    rule belief: dropped_off_crate new_desire: has_crate strength: 2.0;
    rule belief: battery_low new_desire: go_to_charge strength: 2.0;

    action avoid {
        if(obstaclesgeom = nil) {
            write "> Error";
            return;
        }
    }
    
    action consume_battery {
        battery_level <- battery_level - 0.4;
        write "Battery level: " + battery_level;
    }

    plan go_to_crate intention: has_crate {
    	write get_current_intention_op(self);
        do consume_battery;
        if (battery_level < low_battery_threshold) {
            do add_belief(battery_low);
            do remove_desire(has_crate);
            return;
        }
        if (target = nil) {
            do add_subintention(get_current_intention(), select_crate, true);
            do current_intention_on_hold();
        } else {
            if (avoidrect overlaps obstaclesgeom) {
                if (direction = 0) {
                    direction <- rnd(1) = 1 ? 1 : -1;
                    write "Direction chosen: " + direction;
                }
                heading <- heading - (30 * direction);
                write "Updated heading: " + heading;
            } else if (siderect overlaps obstaclesgeom) {
                direction <- 0;
                write "> Avoiding side obstacle!";
            } else {
                direction <- 0;
                write "> Path clear!";
                do rotate;
            }
            do move speed: speed heading: heading;

            float dist <- check_distance(target, location);
            write "Distance to target: " + dist;
            if (dist < 5) {
                write "> Crate collected";
                do add_belief(has_crate);
                target <- nil;
            }
        }
    }

    plan go_to_charge intention: go_to_charge {
    	write get_current_intention_op(self);
	    target <- charging_point collect each.location at 0;
	    do rotate;
	    do move speed: speed heading: heading;
	
	    if (avoidrect overlaps obstaclesgeom) {
	        if (direction = 0) {
	            direction <- rnd(1) = 1 ? 1 : -1;
	            write "Direction chosen: " + direction;
	        }
	        heading <- heading - (30 * direction);
	        write "Updated heading: " + heading;
	    } else if (siderect overlaps obstaclesgeom) {
	        direction <- 0;
	        write "> Avoiding side obstacle!";
	    } else {
	        direction <- 0;
	        write "> Path clear!";
	        do rotate;
	    }
	
	    float dist <- check_distance(target, location);
	    if (dist < 5) {
	        write "> Charging...";
	        battery_level <- 100.0;
	        target <- nil;
	        do remove_belief(battery_low);
	        do remove_intention(go_to_charge);
	        do add_desire(has_crate);
	        write "> Battery fully charged. Returning to work.";
	        return;       
    }
}


    float check_distance(point p0, point p1) {
        float diff <- abs(p0.x - p1.x);
        diff <- diff + abs(p0.y - p1.y);
        return diff;
    }

	action rotate{
		if (target != nil){
			point d <- target - location;
			float angle <- atan2(d.y, d.x);
			heading <- angle;
		}
	}

	plan choose_crate intention: select_crate instantaneous: true {
    list<crate> crate_objects <- crate collect each;
    target_crate <- nil;

    loop c over: crate_objects {
        if (c.select) {
            target_crate <- c;
            c.select <- false;
            break;
        }
    }

    if (target_crate != nil) {
        target <- target_crate.location;
        write "Target crate set to: " + target;
    } else {
        write "No crates available for selection.";
    }

    do rotate;
    do remove_intention(select_crate, true);
}




	plan deliver_crate intention: dropped_off_crate {
        write get_current_intention_op(self);
        do consume_battery;
        if (target = nil) {
            if (target_crate.is_rotting) {
                write "> Crate is rotting. Delivering to triaging point.";
                target <- triaging_point collect each.location at 0;
            } else {
                target <- drop_off_point collect each.location at 0;
            }
            do rotate;
        } else {
            do move speed: speed heading: heading;

            if (avoidrect overlaps obstaclesgeom) {
                if (direction = 0) {
                    direction <- rnd(1) = 1 ? 1 : -1;
                    write "Direction chosen: " + direction;
                }
                heading <- heading - (30 * direction);
                write "Updated heading: " + heading;
            } else if (siderect overlaps obstaclesgeom) {
                direction <- 0;
                write "> Avoiding side obstacle!";
            } else {
                direction <- 0;
                write "> Path clear!";
                do rotate;
            }

            float dist <- check_distance(target, location);
            if (dist < 5) {
                if (target_crate.is_rotting) {
                    write "> Rotting crate disposed at triaging point.";
                } else {
                    write "> Crate delivered to drop-off point.";
                }
                do remove_belief(has_crate);
                crates_visited <- crates_visited + 1;
                target <- nil;
                do add_belief(dropped_off_crate);
            }
        }
	}

	float check_distance(point p0, point p1){
		float diff <- abs(p0.x - p1.x);
		diff <- diff + abs(p0.y - p1.y);
		return diff;
	}

}

species triaging_point {
    float size <- 7.0;
    rgb color <- #red;
    geometry geo <- square(size);

    init {
        location <- point(50, 50); // Posizione arbitraria
    }

    aspect base {
        draw geo at: location color: color;
        draw "triaging point" color:#black;
    }
}

species crate {
	float size <- 3.0;
	rgb color <- #purple;
	geometry geo <- square(size);
	bool select;
	bool is_rotting;

	init {
		location <- point(80, 30);
		select <- true;
		is_rotting <- rnd(1) < 0.2;
	}

	aspect base {
		draw geo at: location color: color;
	}
}

species obstacle {
	float width <- 3.0;
	float height <- 20.0;
	rgb color <- #brown;
	geometry geo <- rectangle(width , height) rotated_by 0;

	aspect base {
		draw geo at: location color: color;
	}
}

species drop_off_point {
	float size <- 8.0;
	rgb color <- #green;
	geometry geo <- square(size);
	bool select;

	init {
		location <- point(80, 80);
	}

	aspect base {
		draw geo at: location color: color;
		draw "drop off point" color:#black;

	}
}

species charging_point {
	float size <- 7.0;
	rgb color <- #yellow;
	geometry geo <- square(size);
	bool select;

	init {
		location <- point(20, 80);
	}

	aspect base {
		draw geo at: location color: color;
		draw "Charging station" color:#black;

	}
}

experiment crates type: gui{
		parameter "rotation" var: rotation min:0 max:359 ;
		parameter "obstacle_count" var: obstacle_count;
		parameter "crate count" var: crate_count;
		output {
			display main_display type: 2d background: #white{
			species robot aspect: base;
			species crate aspect: base;
			species obstacle aspect: base;
			species drop_off_point aspect: base;
			species charging_point aspect:base;
			species triaging_point aspect: base;
		}
	}
}


