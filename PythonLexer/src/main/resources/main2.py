from enum import Enum
import graphics
import math


class Point:

    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.pict = graphics.Circle(graphics.Point(x, y), 3)
        self.set_color("black")

    def set_color(self, color):
        self.pict.setFill(color)
        self.pict.setOutline(color)

    def draw(self, window):
        self.pict.draw(window)

    def undraw(self):
        self.pict.undraw()


class Line:

    class Location(Enum):
        LEFT = 1
        ON = 2
        RIGHT = 3

    def __init__(self, p1, p2):
        self.p1 = p1
        self.p2 = p2
        self.pict = graphics.Line(graphics.Point(p1.x, p1.y), graphics.Point(p2.x, p2.y))
        self.pict.setWidth(2)
        self.set_color("red")

    def set_color(self, color):
        self.pict.setFill(color)
        self.pict.setOutline(color)

    def draw(self, window):
        self.pict.draw(window)

    def undraw(self):
        self.pict.undraw()

    def discriminate(self, point):
        mark = (point.x - self.p1.x) * (self.p2.y - self.p1.y) - (point.y - self.p1.y) * (self.p2.x - self.p1.x)
        if mark < 0:
            return Line.Location.LEFT
        elif mark > 0:
            return Line.Location.RIGHT
        else:
            return Line.Location.ON


class ConvexHull:

    def __init__(self, points):
        self.points = points
        self.lines = []
        for i in range(len(points)):
            self.lines.append(Line(self.points[i], self.points[(i + 1) % len(points)]))

    def draw(self, window):
        for line in self.lines:
            line.draw(window)
        for point in self.points:
            point.undraw()
            point.set_color("red")
            point.draw(window)

    def undraw(self, window):
        for line in self.lines:
            line.undraw()
        for point in self.points:
            point.undraw()
            point.set_color("black")
            point.draw(window)


class Operations:

    @staticmethod
    def length(p1, p2):
        return math.sqrt((p1.x - p2.x) ** 2 + (p1.y - p2.y) ** 2)

    @staticmethod
    def square(p1, p2, p3):
        a = Operations.length(p1, p2)
        b = Operations.length(p2, p3)
        c = Operations.length(p1, p3)
        p = (a + b + c) / 2
        return math.sqrt(p * (p - a) * (p - b) * (p - c))

    @staticmethod
    def polar_angle(origin, point):
        x = point.x - origin.x
        y = point.y - origin.y

        if x != 0:
            return math.atan2(y, x)
        else:
            if y > 0:
                return math.pi / 2
            elif y < 0:
                return 3 * math.pi / 2
            else:
                return None

    @staticmethod
    def sort_by_polar_angle(points):
        x_average = sum(p.x for p in points) / len(points)
        y_average = sum(p.y for p in points) / len(points)
        centroid = Point(x_average, y_average)
        points.sort(key=lambda p: Operations.polar_angle(centroid, p))


class QuickHull:
    epsilon = 1

    @staticmethod
    def execute(points):
        if len(points) == 0:
            return None

        first_point = min(points, key=lambda p: p.x)
        second_point = Point(first_point.x, first_point.y - QuickHull.epsilon)
        points.append(second_point)

        result = QuickHull.__quickhull(points, Line(first_point, second_point))
        points.remove(second_point)
        result.remove(second_point)

        return ConvexHull(result)

    @staticmethod
    def __quickhull(points, line):
        if len(points) == 2:
            Operations.sort_by_polar_angle(points)
            return points

        farthest_point = max(points, key=lambda p: Operations.square(p, line.p1, line.p2))
        left_line = Line(line.p1, farthest_point)
        right_line = Line(farthest_point, line.p2)
        left_points = []
        right_points = []

        for point in points:
            pos_left = left_line.discriminate(point)
            pos_right = right_line.discriminate(point)
            if pos_left == Line.Location.LEFT or pos_left == Line.Location.ON:
                left_points.append(point)
            if pos_right == Line.Location.LEFT or pos_right == Line.Location.ON:
                right_points.append(point)

        result_left = QuickHull.__quickhull(left_points, left_line)
        result_right = QuickHull.__quickhull(right_points, right_line)
        result_right.remove(farthest_point)

        return result_left + result_right


class Main:
    win = graphics.GraphWin("QuickHull", 1000, 700)
    points = []
    hull = None

    @staticmethod
    def on_mouse_click(event):
        point = Point(event.x, event.y)
        if any(point.x == p.x and point.y == p.y for p in Main.points):
            return
        Main.points.append(point)
        point.draw(Main.win)

        if len(Main.points) > 2:
            if Main.hull is not None:
                Main.hull.undraw(Main.win)
            Main.hull = QuickHull.execute(Main.points)
            Main.hull.draw(Main.win)

    @staticmethod
    def main():
        Main.win.setMouseHandler(Main.on_mouse_click)
        Main.win.mainloop()


if __name__ == '__main__':
    Main.main()
